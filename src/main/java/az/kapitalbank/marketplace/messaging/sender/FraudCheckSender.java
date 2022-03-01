package az.kapitalbank.marketplace.messaging.sender;

import java.util.LinkedList;
import java.util.function.Supplier;

import az.kapitalbank.marketplace.messaging.event.FraudCheckEvent;
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
@FieldDefaults(level = AccessLevel.PRIVATE)
public class FraudCheckSender {

    final ObjectMapper objectMapper;

    LinkedList<FraudCheckEvent> fraudCheckEventLinkedList = new LinkedList<>();

    public void sendMessage(FraudCheckEvent fraudCheckEvent) {
        if (fraudCheckEventLinkedList == null) {
            fraudCheckEventLinkedList = new LinkedList<>();
        }
        fraudCheckEventLinkedList.push(fraudCheckEvent);
    }

    @Bean
    public Supplier<String> checkFraud() {
        log.info("Fraud check is produced to topic...");
        return () -> {
            if (fraudCheckEventLinkedList.peek() != null) {
                try {
                    var jsonMessage = objectMapper.writeValueAsString(fraudCheckEventLinkedList.peek());
                    log.info("Fraud check was produced. Message - {}", jsonMessage);
                    fraudCheckEventLinkedList.poll();
                    return jsonMessage;
                } catch (JsonProcessingException j) {
                    log.info("customer check fraud producer. JsonProcessingException - {}", j.getMessage());
                }
            }
            return null;
        };
    }

}
