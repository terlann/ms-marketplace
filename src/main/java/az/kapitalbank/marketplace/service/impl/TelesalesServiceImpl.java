package az.kapitalbank.marketplace.service.impl;

import az.kapitalbank.marketplace.client.telesales.TelesalesClient;
import az.kapitalbank.marketplace.mappers.TelesalesMapper;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import az.kapitalbank.marketplace.repository.FraudRepository;
import az.kapitalbank.marketplace.repository.OrderRepository;
import az.kapitalbank.marketplace.service.TelesalesService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class TelesalesServiceImpl implements TelesalesService {

    TelesalesClient telesalesClient;

    TelesalesMapper telesalesMapper;

    OrderRepository orderRepository;
    CustomerRepository customerRepository;
    FraudRepository fraudRepository;

    @Override
    public Optional<String> sendLead(String trackId) {
        log.error("send lead to telesales start... track_id -[{}]", trackId);

        var customerEntity = customerRepository.findById(trackId).get();
        var orderEntity = orderRepository.findById(trackId).get();
        var fraudReasons = fraudRepository.getAllSuspiciousFraudReasonByTrackId(trackId);
        var createTelesalesOrderRequest = telesalesMapper
                .toTelesalesOrder(customerEntity, orderEntity, fraudReasons);
        try {
            var createTelesalesOrderResponse = telesalesClient.sendLead(createTelesalesOrderRequest);
            log.error("send lead to telesales finish... track_id -[{}], Response - {}",
                    trackId,
                    createTelesalesOrderResponse);
            return Optional.of(createTelesalesOrderResponse.getOperationId());
        } catch (Exception e) {
            log.error("cannot send lead to telesales. track_id -[{}], Exception - {}", trackId, e.getMessage());
        }
        return Optional.empty();
    }
}
