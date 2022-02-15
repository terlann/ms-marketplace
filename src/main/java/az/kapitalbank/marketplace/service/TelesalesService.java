package az.kapitalbank.marketplace.service;

import java.util.Optional;
import java.util.UUID;

import az.kapitalbank.marketplace.client.telesales.TelesalesClient;
import az.kapitalbank.marketplace.mappers.TelesalesMapper;
import az.kapitalbank.marketplace.repository.FraudRepository;
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
    FraudRepository fraudRepository;
    OperationRepository operationRepository;

    public Optional<String> sendLead(UUID trackId) {
        log.error("send lead to telesales start... track_id -[{}]", trackId);

        try {
            var operationEntityOptional = operationRepository.findById(trackId);
            if (operationEntityOptional.isPresent()) {
                var operationEntity = operationEntityOptional.get();
                var fraudReasons = fraudRepository.getAllSuspiciousFraudReasonByTrackId(trackId);
                var createTelesalesOrderRequest = telesalesMapper
                        .toTelesalesOrder(operationEntity, fraudReasons);
                var amountWithCommission = operationEntity.getTotalAmount().add(operationEntity.getCommission());
                createTelesalesOrderRequest.setLoanAmount(amountWithCommission);
                var createTelesalesOrderResponse = telesalesClient.sendLead(createTelesalesOrderRequest);
                log.error("send lead to telesales finish... track_id -[{}], Response - {}",
                        trackId,
                        createTelesalesOrderResponse);
                return Optional.of(createTelesalesOrderResponse.getOperationId());
            }
        } catch (Exception e) {
            log.error("cannot send lead to telesales. track_id -[{}], Exception - {}", trackId, e.getMessage());
        }
        return Optional.empty();
    }
}
