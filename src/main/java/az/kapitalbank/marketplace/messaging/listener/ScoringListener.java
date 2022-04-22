package az.kapitalbank.marketplace.messaging.listener;

import az.kapitalbank.marketplace.messaging.event.ScoringResultEvent;
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
public class ScoringListener {

    LoanFormalizationService loanFormalizationService;

    @Bean
    public Consumer<ScoringResultEvent> scoringResult() {
        return event -> {
            if (Objects.nonNull(event)) {
                log.info("Scoring result consumer is started. Event - {}", event);
                loanFormalizationService.scoringResultProcess(event);
            }
        };
    }
}
