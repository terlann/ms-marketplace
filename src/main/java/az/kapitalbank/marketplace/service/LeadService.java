package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constant.TelesalesConstant.UMICO_SOURCE_CODE;

import az.kapitalbank.marketplace.client.loan.LoanClient;
import az.kapitalbank.marketplace.client.telesales.TelesalesClient;
import az.kapitalbank.marketplace.constant.FraudType;
import az.kapitalbank.marketplace.constant.ProductType;
import az.kapitalbank.marketplace.constant.SubProductType;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.dto.request.LoanRequest;
import az.kapitalbank.marketplace.dto.response.LoanResponse;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.mapper.TelesalesMapper;
import java.util.List;
import java.util.Optional;
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

    LoanClient loanClient;
    UmicoService umicoService;
    TelesalesClient telesalesClient;
    TelesalesMapper telesalesMapper;

    private Optional<String> sendLeadTelesales(OperationEntity operationEntity,
                                               List<FraudType> fraudTypes) {
        var trackId = operationEntity.getId();
        log.info("Send lead to telesales is started : trackId - {}", trackId);
        try {
            var createTelesalesOrderRequest =
                    telesalesMapper.toTelesalesOrder(operationEntity, fraudTypes);
            var amountWithCommission =
                    operationEntity.getTotalAmount().add(operationEntity.getCommission());
            createTelesalesOrderRequest.setLoanAmount(amountWithCommission);
            log.info("Send lead to telesales : request - {}", createTelesalesOrderRequest);
            var createTelesalesOrderResponse =
                    telesalesClient.sendLead(createTelesalesOrderRequest);
            var response = createTelesalesOrderResponse.getResponse().getMessage();
            if ((response.equals("Request not readable"))
                    || (response.equals("Internal Server Error"))) {
                log.error("Send lead to telesales was failed , error - Request not readable :"
                        + " trackId - {}, exception - {}", trackId, response);
                operationEntity.setSendTelesalesError(response);
                return Optional.empty();
            }
            log.info("Send lead to telesales was finished :" + " trackId - {}, response - {}",
                    trackId, createTelesalesOrderResponse);
            return Optional.of(createTelesalesOrderResponse.getOperationId());
        } catch (Exception e) {
            log.error("Send lead to telesales was failed :" + " trackId - {}, exception - {}",
                    trackId, e);
            operationEntity.setSendTelesalesError(e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<String> sendLeadLoan(OperationEntity operationEntity) {
        var trackId = operationEntity.getId();
        try {
            log.info("Send lead to loan is started : trackId - {}", trackId);
            LoanRequest loanRequest = LoanRequest.builder().productType(ProductType.BIRKART)
                    .subProductType(SubProductType.UMICO)
                    .phoneNumber(operationEntity.getMobileNumber())
                    .fullName(operationEntity.getFullName()).pinCode(operationEntity.getPin())
                    .productAmount(
                            operationEntity.getTotalAmount().add(operationEntity.getCommission()))
                    .build();
            log.info("Send lead to loan : request - {}", loanRequest);
            LoanResponse response = loanClient.sendLead(UMICO_SOURCE_CODE, loanRequest);
            log.info("Send lead to loan was finished : trackId - {}, response - {}", trackId,
                    response);
            return Optional.of(response.getData().getLeadId());
        } catch (Exception e) {
            log.error("Send lead to loan was failed : trackId - {}, exception - {}", trackId, e);
            return Optional.empty();
        }
    }

    public void sendLead(OperationEntity operationEntity, List<FraudType> fraudTypes) {
        var telesalesOrderId = sendLeadTelesales(operationEntity, fraudTypes);
        telesalesOrderId.ifPresent(operationEntity::setTelesalesOrderId);
        var leadId = sendLeadLoan(operationEntity);
        leadId.ifPresent(operationEntity::setLeadId);
        var sendDecision = umicoService.sendPendingDecision(operationEntity.getId());
        sendDecision.ifPresent(operationEntity::setUmicoDecisionError);
        operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.PENDING);
    }
}
