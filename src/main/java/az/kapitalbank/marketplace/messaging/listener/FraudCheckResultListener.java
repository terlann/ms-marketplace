package az.kapitalbank.marketplace.messaging.listener;

import java.util.Objects;
import java.util.function.Consumer;

import az.kapitalbank.marketplace.messaging.event.FraudCheckResultEvent;
import az.kapitalbank.marketplace.service.ScoringService;
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
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FraudCheckResultListener {

    ObjectMapper objectMapper;
    ScoringService scoringService;

    @Bean
    public Consumer<String> checkFraudResult() {
        return message -> {
            if (Objects.nonNull(message)) {
                try {
                    var fraudCheckResultEvent = objectMapper.readValue(message, FraudCheckResultEvent.class);
                    log.info("check fraud result consumer. Message - {}", fraudCheckResultEvent);
                    scoringService.fraudResultProcess(fraudCheckResultEvent);
                } catch (JsonProcessingException j) {
                    log.error("check fraud result consume.Message - {}, JsonProcessingException - {}",
                            message,
                            j.getMessage());
                }
            }

        };
    }
}
