package az.kapitalbank.marketplace.messaging.listener;

import az.kapitalbank.marketplace.messaging.event.FraudCheckResultEvent;
import az.kapitalbank.marketplace.service.ProductProcessService;
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
public class FraudListener {

    ObjectMapper objectMapper;
    ProductProcessService productProcessService;

    @Bean
    public Consumer<String> checkFraudResult() {
        return message -> {
            if (Objects.nonNull(message)) {
                try {
                    var fraudCheckResultEvent =
                            objectMapper.readValue(message, FraudCheckResultEvent.class);
                    log.info("check fraud result consumer. Message - {}", fraudCheckResultEvent);
                    productProcessService.fraudResultProcess(fraudCheckResultEvent);
                } catch (Exception ex) {
                    log.error(
                            "Exception check fraud result consume.Message - {}, Exception - {}",
                            message,
                            ex);
                }
            }
        };
    }
}
