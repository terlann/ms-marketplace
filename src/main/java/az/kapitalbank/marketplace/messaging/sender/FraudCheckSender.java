package az.kapitalbank.marketplace.messaging.sender;

import az.kapitalbank.marketplace.messaging.event.FraudCheckEvent;
import java.util.LinkedList;
import java.util.function.Supplier;
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

    final LinkedList<FraudCheckEvent> fraudCheckEvents = new LinkedList<>();

    public void sendMessage(FraudCheckEvent fraudCheckEvent) {
        fraudCheckEvents.push(fraudCheckEvent);
    }

    @Bean
    public Supplier<FraudCheckEvent> checkFraud() {
        log.info("Fraud check is produced to topic...");
        return () -> {
            if (fraudCheckEvents.peek() != null) {
                var message = fraudCheckEvents.peek();
                log.info("Fraud check was produced. Message - {}", message);
                fraudCheckEvents.poll();
                return message;
            }
            return null;
        };
    }

}
