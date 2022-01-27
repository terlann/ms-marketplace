package az.kapitalbank.marketplace.service;

import java.util.Optional;
import java.util.UUID;

import az.kapitalbank.marketplace.client.telesales.TelesalesClient;
import az.kapitalbank.marketplace.mappers.TelesalesMapper;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import az.kapitalbank.marketplace.repository.FraudRepository;
import az.kapitalbank.marketplace.repository.OperationRepository;
import az.kapitalbank.marketplace.repository.OrderRepository;
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

    OrderRepository orderRepository;
    CustomerRepository customerRepository;
    FraudRepository fraudRepository;
    OperationRepository operationRepository;

    public Optional<String> sendLead(UUID trackId) {
        log.error("send lead to telesales start... track_id -[{}]", trackId);

        try {
            var operationEntity = operationRepository.findById(trackId);
            if (operationEntity.isPresent()) {
                var customerEntity = operationEntity.get().getCustomer();
                var fraudReasons = fraudRepository.getAllSuspiciousFraudReasonByTrackId(trackId);
                //TODO fix telesalesMapper and unused everything
                var createTelesalesOrderRequest = telesalesMapper
                        .toTelesalesOrder(customerEntity, operationEntity.get(), fraudReasons);
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
