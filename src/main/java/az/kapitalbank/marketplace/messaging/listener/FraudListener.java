package az.kapitalbank.marketplace.messaging.listener;

import az.kapitalbank.marketplace.messaging.event.FraudCheckResultEvent;
import az.kapitalbank.marketplace.service.LoanFormalizationService;
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

    LoanFormalizationService loanFormalizationService;

    @Bean
    public Consumer<FraudCheckResultEvent> checkFraudResult() {
        return event -> {
            if (Objects.nonNull(event)) {
                log.info("Check fraud result consumer is started. Event - {}", event);
                loanFormalizationService.fraudResultProcess(event);
            }
        };
    }
}
