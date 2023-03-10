package az.kapitalbank.marketplace.messaging.publisher;

import az.kapitalbank.marketplace.messaging.event.PrePurchaseEvent;
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
public class PrePurchasePublisher {

    final LinkedList<PrePurchaseEvent> prePurchaseEvents = new LinkedList<>();

    public void sendEvent(PrePurchaseEvent prePurchaseEvent) {
        prePurchaseEvents.push(prePurchaseEvent);
    }

    @Bean
    public Supplier<PrePurchaseEvent> prePurchase() {
        return () -> {
            if (prePurchaseEvents.peek() != null) {
                var event = prePurchaseEvents.peek();
                log.info("Pre purchase was published. Event - {}", event);
                prePurchaseEvents.poll();
                return event;
            }
            return null;
        };
    }

}
