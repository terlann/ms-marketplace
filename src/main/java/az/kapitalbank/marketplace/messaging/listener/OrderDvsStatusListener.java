package az.kapitalbank.marketplace.messaging.listener;

import java.util.Objects;
import java.util.function.Consumer;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.dto.CompleteScoring;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.messaging.event.OrderDvsStatusEvent;
import az.kapitalbank.marketplace.repository.OperationRepository;
import az.kapitalbank.marketplace.service.ScoringService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderDvsStatusListener {

    AtlasClient atlasClient;
    ObjectMapper objectMapper;
    ScoringService scoringService;
    OperationRepository operationRepository;

    @Bean
    public Consumer<String> orderDvsStatus() {
        return message -> {
            if (Objects.nonNull(message)) {
                try {
                    OrderDvsStatusEvent orderDvsStatusEvent = objectMapper
                            .readValue(message, OrderDvsStatusEvent.class);
                    log.info("order dvs status consumer. Message - [{}]", orderDvsStatusEvent);
                    if (orderDvsStatusEvent != null) {
                        var operationEntityOptional = operationRepository.findByDvsOrderId(
                                orderDvsStatusEvent.getOrderId());
                        if (operationEntityOptional.isPresent()) {
                            var operationEntity = operationEntityOptional.get();
                            var customerEntity = operationEntity.getCustomer();
                            var completeScoring = CompleteScoring.builder()
                                    .trackId(operationEntity.getId())
                                    .businessKey(operationEntity.getBusinessKey())
                                    .additionalNumber1(customerEntity.getAdditionalPhoneNumber1())
                                    .additionalNumber2(customerEntity.getAdditionalPhoneNumber2())
                                    .build();
                            scoringService.completeScoring(completeScoring);
                            var orders = operationEntity.getOrders();
                            //TODO mapper for atlasClient
                            for (OrderEntity orderEntity : orders) {
                                atlasClient.purchase(null);
                            }
                            // TODO send desicion to umico (umico callback url)
                        }
                    }
                } catch (JsonProcessingException j) {
                    log.error("order dvs status consume.Message - [{}], JsonProcessingException - {}",
                            message,
                            j.getMessage());
                }
            }
        };
    }
}
