package az.kapitalbank.marketplace.messaging.listener;

import az.kapitalbank.marketplace.messaging.event.ScoringResultEvent;
import az.kapitalbank.marketplace.service.ScoringService;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    ObjectMapper objectMapper;
    ScoringService scoringService;

    @Bean
    public Consumer<String> scoringResult() {
        return message -> {
            if (Objects.nonNull(message)) {
                try {
                    var startScoringResult =
                            objectMapper.readValue(message, ScoringResultEvent.class);
                    log.info("scoring result consumer. Message - {}", startScoringResult);
                    scoringService.scoringResultProcess(startScoringResult);
                } catch (Exception ex) {
                    log.error("Exception scoring result consume.Message - {}, Exception - {}",
                            message,
                            ex.getMessage());
                }
            }
        };
    }
}
