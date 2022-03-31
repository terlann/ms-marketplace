package az.kapitalbank.marketplace.messaging.listener;

import az.kapitalbank.marketplace.messaging.event.VerificationResultEvent;
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
public class VerificationListener {

    ObjectMapper objectMapper;
    ProductProcessService productProcessService;

    @Bean
    public Consumer<String> verificationResult() {
        return message -> {
            if (Objects.nonNull(message)) {
                try {
                    var verificationResultEvent =
                            objectMapper.readValue(message, VerificationResultEvent.class);
                    log.info("Verification status consumer. Message - {}", verificationResultEvent);
                    productProcessService.verificationResultProcess(verificationResultEvent);
                } catch (Exception ex) {
                    log.error("Exception verification status consume.Message - {},"
                            + " Exception - {}", message, ex);
                }
            }
        };
    }


}
