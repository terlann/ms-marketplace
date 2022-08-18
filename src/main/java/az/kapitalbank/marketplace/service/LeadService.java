package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constant.TelesalesConstant.UMICO_SOURCE_CODE;
import static az.kapitalbank.marketplace.constant.UmicoDecisionStatus.FAIL_IN_PREAPPROVED;
import static az.kapitalbank.marketplace.constant.UmicoDecisionStatus.PENDING;
import static az.kapitalbank.marketplace.constant.UmicoDecisionStatus.PREAPPROVED;

import az.kapitalbank.marketplace.client.loan.LoanClient;
import az.kapitalbank.marketplace.client.loan.model.LoanRequest;
import az.kapitalbank.marketplace.client.loan.model.LoanResponse;
import az.kapitalbank.marketplace.client.telesales.TelesalesClient;
import az.kapitalbank.marketplace.constant.ProcessStatus;
import az.kapitalbank.marketplace.constant.ProductType;
import az.kapitalbank.marketplace.constant.SendLeadType;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.ProcessStepEntity;
import az.kapitalbank.marketplace.mapper.OrderMapper;
import az.kapitalbank.marketplace.mapper.TelesalesMapper;
import az.kapitalbank.marketplace.messaging.publisher.FraudCheckPublisher;
import az.kapitalbank.marketplace.repository.OperationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class LeadService {

    SmsService smsService;
    LoanClient loanClient;
    OrderMapper orderMapper;
    UmicoService umicoService;
    TelesalesClient telesalesClient;
    OperationRepository operationRepository;
    FraudCheckPublisher fraudCheckPublisher;
    TelesalesMapper telesalesMapper;

    private Optional<String> sendLeadTelesales(OperationEntity operationEntity) {
        var trackId = operationEntity.getId();
        try {
            var request = telesalesMapper.toTelesalesOrder(operationEntity);
            log.info("Send lead to telesales is started : trackId - {}, request - {}",
                    trackId, request);
            var createTelesalesOrderResponse =
                    telesalesClient.sendLead(request);
            var responseMessage = createTelesalesOrderResponse.getResponse().getMessage();
            var responseCode = createTelesalesOrderResponse.getResponse().getCode();
            if (!responseCode.equals("0")) {
                log.error("Send lead to telesales was failed : trackId - {}, exception - {}",
                        trackId, responseMessage);
                operationEntity.setIsSendLead(false);
                return Optional.empty();
            }
            operationEntity.setIsSendLead(true);
            log.info("Send lead to telesales was finished :" + " trackId - {}, response - {}",
                    trackId, createTelesalesOrderResponse);
            return Optional.of(createTelesalesOrderResponse.getOperationId());
        } catch (Exception e) {
            log.error("Send lead to telesales was failed :"
                    + " trackId - {}, exception - {}", trackId, e);
            operationEntity.setIsSendLead(false);
            return Optional.empty();
        }
    }


    private Optional<String> sendLeadLoan(OperationEntity operationEntity) {
        var trackId = operationEntity.getId();
        try {
            var monthlyPayment =
                    operationEntity.getTotalAmount().add(operationEntity.getCommission())
                            .divide(BigDecimal.valueOf(operationEntity.getLoanTerm()),
                                    2, RoundingMode.FLOOR);
            LoanRequest loanRequest =
                    LoanRequest.builder().productType(ProductType.UMICO_MARKETPLACE)
                            .phoneNumber(operationEntity.getMobileNumber())
                            .fullName(operationEntity.getFullName())
                            .pinCode(operationEntity.getPin())
                            .productAmount(operationEntity.getTotalAmount()
                                    .add(operationEntity.getCommission()))
                            .monthlyPayment(monthlyPayment)
                            .umicoUserId(operationEntity.getCustomer().getUmicoUserId())
                            .productDuration(operationEntity.getLoanTerm())
                            .leadComment(operationEntity.getProcessStatus()).build();
            log.info("Send lead to loan is started : trackId - {}, request - {}",
                    trackId, loanRequest);
            LoanResponse response = loanClient.sendLead(UMICO_SOURCE_CODE, loanRequest);
            log.info("Send lead to loan was finished : trackId - {}, response - {}", trackId,
                    response);
            return Optional.of(response.getData().getLeadId());
        } catch (Exception e) {
            log.error("Send lead to loan was failed : trackId - {}, exception - {}", trackId, e);
            return Optional.empty();
        }
    }

    public void sendLead(OperationEntity operationEntity) {
        sendLeadLoan(operationEntity);
        var telesalesOrderId = sendLeadTelesales(operationEntity);
        telesalesOrderId.ifPresent(operationEntity::setTelesalesOrderId);
        if (operationEntity.getUmicoDecisionStatus() != PENDING) {
            var umicoDecisionStatus = umicoService.sendPendingDecision(operationEntity.getId());
            smsService.sendPendingSms(operationEntity);
            operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
        }
    }

    @Transactional
    public void sendLeadNoActionDvs() {
        var operations = operationRepository.findByUpdatedAtBeforeAndUmicoDecisionStatusIn(
                OffsetDateTime.now().minusMinutes(20),
                Set.of(PREAPPROVED, FAIL_IN_PREAPPROVED));
        operations.forEach(operation -> {
            var processStatus = ProcessStatus.DVS_NO_ACTION;
            operation.setProcessStatus(processStatus);
            var processStep = ProcessStepEntity.builder().value(processStatus).build();
            operation.setProcessSteps(Collections.singletonList(processStep));
            processStep.setOperation(operation);
            sendLead(operation);
            log.info("Send lead schedule process was finished : trackId - {}", operation.getId());
        });
    }

    @Transactional
    public void retrySendLead() {
        List<OperationEntity> operationEntities = operationRepository
                .findByUmicoDecisionStatusAndIsSendLeadIsFalse(UmicoDecisionStatus.PENDING);
        operationEntities.forEach(this::sendLead);
    }

    @Transactional
    public void sendLeadManual(SendLeadType sendLeadType) {
        log.info("Send lead manual process is started : sendLeadType - {}", sendLeadType);
        if (sendLeadType == SendLeadType.SEND_LEAD_FAILED) {
            retrySendLead();
        } else if (sendLeadType == SendLeadType.NO_ACTION_DVS) {
            sendLeadNoActionDvs();
        }
        log.info("Send lead manual process was finished : sendLeadType - {}", sendLeadType);
    }
}
