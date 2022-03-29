package az.kapitalbank.marketplace.service;

import az.kapitalbank.marketplace.client.loan.LoanClient;
import az.kapitalbank.marketplace.client.telesales.TelesalesClient;
import az.kapitalbank.marketplace.constant.ProductType;
import az.kapitalbank.marketplace.constant.SubProductType;
import az.kapitalbank.marketplace.dto.LeadDto;
import az.kapitalbank.marketplace.dto.request.LoanRequest;
import az.kapitalbank.marketplace.dto.response.LoanResponse;
import az.kapitalbank.marketplace.exception.OperationNotFoundException;
import az.kapitalbank.marketplace.mapper.TelesalesMapper;
import az.kapitalbank.marketplace.repository.OperationRepository;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class TelesalesService {

    TelesalesClient telesalesClient;
    TelesalesMapper telesalesMapper;
    OperationRepository operationRepository;
    LoanClient loanClient;

    public Optional<String> sendLead(LeadDto leadDto) {
        var trackId = leadDto.getTrackId();
        log.info("Send lead to telesales is started : trackId - {}", trackId);
        try {
            var operationEntity = operationRepository.findById(trackId)
                    .orElseThrow(() -> new OperationNotFoundException("trackId - " + trackId));
            var fraudTypes = leadDto.getTypes();
            var createTelesalesOrderRequest = telesalesMapper
                    .toTelesalesOrder(operationEntity, fraudTypes);
            var amountWithCommission = operationEntity.getTotalAmount().add(operationEntity
                    .getCommission());
            createTelesalesOrderRequest.setLoanAmount(amountWithCommission);
            log.info("Send lead to telesales : request - {}", createTelesalesOrderRequest);
            var createTelesalesOrderResponse =
                    telesalesClient.sendLead(createTelesalesOrderRequest);
            log.info("Send lead to telesales was finished successfully..."
                    + " trackId -{}, Response - {}", trackId);
            log.info("Send lead to telesales was finished : trackId - {}, response - {}",
                    trackId, createTelesalesOrderResponse);
            sendLeadToLoanService(trackId);
            return Optional.of(createTelesalesOrderResponse.getOperationId());
        } catch (Exception e) {
            log.error("Send lead to telesales was finished unsuccessfully."
                            + " trackId -{}, Exception - {}", trackId,
                    e.getMessage());
            return Optional.empty();
        }
    }

    public Optional<LoanResponse> sendLeadToLoanService(UUID trackId) {
        try {
            log.info("Send lead to loan service is started... trackId - {}", trackId);
            String source = "0007";
            var operationEntity = operationRepository.findById(trackId)
                    .orElseThrow(() -> new OperationNotFoundException("trackId - " + trackId));

            LoanRequest loanRequest =
                    LoanRequest.builder()
                            .productType(ProductType.BIRKART)
                            .subProductType(SubProductType.UMICO)
                            .phoneNumber(operationEntity.getMobileNumber())
                            .fullName(operationEntity.getFullName())
                            .pinCode(operationEntity.getPin())
                            .productAmount(operationEntity.getTotalAmount().add(operationEntity
                                    .getCommission()))
                            .build();
            log.info("Send lead to loan service : request - {}", loanRequest);
            LoanResponse response = loanClient.sendLead(source, loanRequest);
            log.info("Send lead to loan service was finished successfully... trackId -{},"
                    + " Response - {}", trackId, response);
            return Optional.of(response);
        } catch (Exception e) {
            log.error("Send lead to loan service was finished unsuccessfully. "
                    + "trackId -{}, Exception - {}", trackId, e.getMessage());
            log.error("Send lead to telesales was failed : trackId - {}, exception - {}",
                    trackId, e);
            return Optional.empty();
        }
    }
}
