package az.kapitalbank.marketplace.messaging.listener;

import java.util.Objects;
import java.util.function.Consumer;

import az.kapitalbank.marketplace.messaging.event.ScoringResultEvent;
import az.kapitalbank.marketplace.service.ProductCreateService;
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
public class ScoringResultListener {

    ObjectMapper objectMapper;
    ProductCreateService productCreateService;

    @Bean
    public Consumer<String> startScoringResult() {
        return message -> {
            if (Objects.nonNull(message)) {
                try {
                    ScoringResultEvent startScoringResult = objectMapper.readValue(message, ScoringResultEvent.class);
                    log.info("start scoring result consumer. Message - [{}]", startScoringResult);

                    productCreateService.createScoring(startScoringResult);
                } catch (JsonProcessingException j) {
                    log.error("start scoring result consume.Message - [{}], JsonProcessingException - {}",
                            message,
                            j.getMessage());
                }
            }
        };
    }
}
