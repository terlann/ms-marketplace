package az.kapitalbank.marketplace.messaging.listener;

import az.kapitalbank.marketplace.messaging.event.FraudCheckResultEvent;
import az.kapitalbank.marketplace.service.ProductProcessService;
import java.util.Objects;
import java.util.function.Consumer;
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
public class FraudListener {

    ProductProcessService productProcessService;

    @Bean
    public Consumer<FraudCheckResultEvent> checkFraudResult() {
        return message -> {
            if (Objects.nonNull(message)) {
                log.info("check fraud result consumer. Message - {}", message);
                productProcessService.fraudResultProcess(message);
            }
        };
    }
}
