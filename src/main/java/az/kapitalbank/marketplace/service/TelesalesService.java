package az.kapitalbank.marketplace.service;

import java.util.Optional;

import az.kapitalbank.marketplace.client.telesales.TelesalesClient;
import az.kapitalbank.marketplace.dto.LeadDto;
import az.kapitalbank.marketplace.exception.OperationNotFoundException;
import az.kapitalbank.marketplace.mapper.TelesalesMapper;
import az.kapitalbank.marketplace.repository.OperationRepository;
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

    public Optional<String> sendLead(LeadDto leadDto) {
        var trackId = leadDto.getTrackId();
        log.info("Send lead to telesales is started... trackId - {}", trackId);
        try {
            var operationEntity = operationRepository.findById(trackId)
                    .orElseThrow(() -> new OperationNotFoundException("trackId - " + trackId));
            var fraudTypes = leadDto.getTypes();
            var createTelesalesOrderRequest = telesalesMapper
                    .toTelesalesOrder(operationEntity, fraudTypes);
            var amountWithCommission = operationEntity.getTotalAmount().add(operationEntity.getCommission());
            createTelesalesOrderRequest.setLoanAmount(amountWithCommission);
            log.info("Send lead to telesales : request - {}", createTelesalesOrderRequest);
            var createTelesalesOrderResponse = telesalesClient.sendLead(createTelesalesOrderRequest);
            log.info("Send lead to telesales was finished successfully... trackId -{}, Response - {}",
                    trackId,
                    createTelesalesOrderResponse);
            return Optional.of(createTelesalesOrderResponse.getOperationId());
        } catch (Exception e) {
            log.error("Send lead to telesales was finished unsuccessfully. trackId -{}, Exception - {}",
                    trackId, e.getMessage());
            return Optional.empty();
        }
    }
}
