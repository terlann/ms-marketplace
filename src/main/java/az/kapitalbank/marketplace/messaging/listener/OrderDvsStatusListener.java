package az.kapitalbank.marketplace.messaging.listener;

import java.util.Objects;
import java.util.function.Consumer;

import az.kapitalbank.marketplace.repository.OrderRepository;
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

    ObjectMapper objectMapper;
    OrderRepository orderRepository;

    @Bean
    public Consumer<String> orderDvsStatus() {
        return message -> {
            if (Objects.nonNull(message)) {

                // TODO call completeScoring
                // TODO send desicion to umico (umico callback url)
                // TODO purchase fimi call

                /*try {
                    OrderDvsStatusEvent orderDvsStatusEvent = objectMapper
                      .readValue(message, OrderDvsStatusEvent.class);
                    log.info("order dvs status consumer. Message - [{}]", orderDvsStatusEvent);
                    if (orderDvsStatusEvent != null)
                        //orderRepository.updateOrderStatusByOrderId(orderDvsStatusEvent.getStatus(),
                                                    orderDvsStatusEvent.getOrderId());
                } catch (JsonProcessingException j) {
                    log.error("order dvs status consume.Message - [{}], JsonProcessingException - {}",
                     message,
                     j.getMessage());
                }*/
            }
        };
    }
}
