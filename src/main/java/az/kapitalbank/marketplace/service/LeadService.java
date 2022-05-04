package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constant.TelesalesConstant.UMICO_SOURCE_CODE;

import az.kapitalbank.marketplace.client.loan.LoanClient;
import az.kapitalbank.marketplace.client.loan.model.LoanRequest;
import az.kapitalbank.marketplace.client.loan.model.LoanResponse;
import az.kapitalbank.marketplace.client.telesales.TelesalesClient;
import az.kapitalbank.marketplace.constant.FraudType;
import az.kapitalbank.marketplace.constant.ProductType;
import az.kapitalbank.marketplace.constant.SubProductType;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.mapper.TelesalesMapper;
import java.util.ArrayList;
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
        try {
            var request = telesalesMapper.toTelesalesOrder(operationEntity, fraudTypes);
            log.info("Send lead to telesales is started : trackId - {}, request - {}",
                    trackId, request);
            var createTelesalesOrderResponse =
                    telesalesClient.sendLead(request);
            var responseMessage = createTelesalesOrderResponse.getResponse().getMessage();
            var responseCode = createTelesalesOrderResponse.getResponse().getCode();
            if (!responseCode.equals("0")) {
                log.error("Send lead to telesales was failed : trackId - {}, exception - {}",
                        trackId, responseMessage);
                operationEntity.setSendLeadTelesales(false);
                return Optional.empty();
            }
            operationEntity.setSendLeadTelesales(true);
            log.info("Send lead to telesales was finished :" + " trackId - {}, response - {}",
                    trackId, createTelesalesOrderResponse);
            return Optional.of(createTelesalesOrderResponse.getOperationId());
        } catch (Exception e) {
            log.error("Send lead to telesales was failed :"
                    + " trackId - {}, exception - {}", trackId, e);
            operationEntity.setSendLeadTelesales(false);
            return Optional.empty();
        }
    }

    public Optional<String> sendLeadLoan(OperationEntity operationEntity) {
        var trackId = operationEntity.getId();
        try {
            LoanRequest loanRequest = LoanRequest.builder().productType(ProductType.BIRKART)
                    .subProductType(SubProductType.UMICO)
                    .phoneNumber(operationEntity.getMobileNumber())
                    .fullName(operationEntity.getFullName()).pinCode(operationEntity.getPin())
                    .productAmount(
                            operationEntity.getTotalAmount()
                                    .add(operationEntity.getCommission()))
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
        var telesalesOrderId = sendLeadTelesales(operationEntity, fraudTypes);
        telesalesOrderId.ifPresent(operationEntity::setTelesalesOrderId);
        var umicoDecisionStatus = umicoService.sendPendingDecision(operationEntity.getId());
        operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
    }
}
