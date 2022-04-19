package az.kapitalbank.marketplace.messaging.sender;

import java.util.LinkedList;
import java.util.UUID;
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
public class PrePurchaseSender {

    final LinkedList<UUID> trackIdList = new LinkedList<>();

    public void sendMessage(UUID trackId) {
        trackIdList.push(trackId);
    }

    @Bean
    public Supplier<UUID> prePurchase() {
        return () -> {
            if (trackIdList.peek() != null) {
                var message = trackIdList.peek();
                log.info("Pre purchase was produced. Message - {}", message);
                trackIdList.poll();
                return message;
            }
            return null;
        };
    }

}
