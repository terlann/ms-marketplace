package az.kapitalbank.marketplace.messaging.publisher;

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
public class FraudCheckPublisher {

    final LinkedList<FraudCheckEvent> fraudCheckEvents = new LinkedList<>();

    public void sendEvent(FraudCheckEvent fraudCheckEvent) {
        fraudCheckEvents.push(fraudCheckEvent);
    }

    @Bean
    public Supplier<FraudCheckEvent> checkFraud() {
        return () -> {
            if (fraudCheckEvents.peek() != null) {
                var event = fraudCheckEvents.peek();
                fraudCheckEvents.poll();
                log.info("Fraud check was published. Event - {}", event);
                return event;
            }
            return null;
        };
    }

}
