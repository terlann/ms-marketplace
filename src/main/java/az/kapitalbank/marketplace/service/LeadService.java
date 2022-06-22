package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constant.TelesalesConstant.UMICO_SOURCE_CODE;
import static az.kapitalbank.marketplace.constant.UmicoDecisionStatus.FAIL_IN_PREAPPROVED;
import static az.kapitalbank.marketplace.constant.UmicoDecisionStatus.PENDING;
import static az.kapitalbank.marketplace.constant.UmicoDecisionStatus.PREAPPROVED;

import az.kapitalbank.marketplace.client.loan.LoanClient;
import az.kapitalbank.marketplace.client.loan.model.LoanRequest;
import az.kapitalbank.marketplace.client.loan.model.LoanResponse;
import az.kapitalbank.marketplace.constant.FraudType;
import az.kapitalbank.marketplace.constant.ProductType;
import az.kapitalbank.marketplace.constant.SendLeadReason;
import az.kapitalbank.marketplace.constant.SendLeadType;
import az.kapitalbank.marketplace.constant.SubProductType;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.mapper.OrderMapper;
import az.kapitalbank.marketplace.messaging.publisher.FraudCheckPublisher;
import az.kapitalbank.marketplace.repository.OperationRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
    OperationRepository operationRepository;
    FraudCheckPublisher fraudCheckPublisher;

    private Optional<String> sendLeadLoan(OperationEntity operationEntity,
                                          List<FraudType> fraudTypes) {
        var trackId = operationEntity.getId();
        try {
            var frauds = fraudTypes.stream().map(Object::toString).collect(Collectors.joining(";"));
            var monthlyPayment =
                    operationEntity.getTotalAmount().add(operationEntity.getCommission())
                            .divide(BigDecimal.valueOf(operationEntity.getLoanTerm()),
                                    2, RoundingMode.FLOOR);
            LoanRequest loanRequest = LoanRequest.builder().productType(ProductType.BIRKART)
                    .subProductType(SubProductType.UMICO)
                    .phoneNumber(operationEntity.getMobileNumber())
                    .fullName(operationEntity.getFullName()).pinCode(operationEntity.getPin())
                    .productAmount(operationEntity.getTotalAmount()
                            .add(operationEntity.getCommission()))
                    .monthlyPayment(monthlyPayment)
                    .umicoUserId(operationEntity.getCustomer().getUmicoUserId())
                    .leadComment(frauds)
                    .build();
            log.info("Send lead to loan is started : trackId - {}, request - {}",
                    trackId, loanRequest);
            LoanResponse response = loanClient.sendLead(UMICO_SOURCE_CODE, loanRequest);
            log.info("Send lead to loan was finished : trackId - {}, response - {}", trackId,
                    response);
            return Optional.of(response.getData().getLeadId());
        } catch (Exception e) {
            log.error("Send lead to loan was failed : trackId - {}, exception - {}", trackId,
                    e);
            return Optional.empty();
        }
    }

    public void sendLead(OperationEntity operationEntity, List<FraudType> fraudTypes) {
        if (fraudTypes == null) {
            fraudTypes = new ArrayList<>();
        }
        var leadId = sendLeadLoan(operationEntity, fraudTypes);
        leadId.ifPresent(operationEntity::setLeadId);
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
            var trackId = operation.getId();
            sendLead(operation, null);
            operation.setSendLeadReason(SendLeadReason.NO_ACTION_DVS);
            log.info("Send lead schedule process was finished : trackId - {}", trackId);
        });
    }

    @Transactional
    public void retrySendLead() {
        List<OperationEntity> operationEntities = operationRepository
                .findByUmicoDecisionStatusAndIsSendLeadIsFalse(UmicoDecisionStatus.PENDING);
        operationEntities.forEach(operationEntity -> {
            if (operationEntity.getSendLeadReason() == SendLeadReason.FRAUD_LIST) {
                var fraudCheckEvent = orderMapper.toFraudCheckEvent(operationEntity);
                fraudCheckPublisher.sendEvent(fraudCheckEvent);
            } else {
                sendLead(operationEntity, null);
            }
        });
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
