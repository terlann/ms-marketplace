package az.kapitalbank.marketplace.service;

import az.kapitalbank.marketplace.client.dvs.DvsClient;
import feign.FeignException;
import java.util.Optional;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class VerificationService {

    DvsClient dvsClient;

    public Optional<String> getDvsUrl(UUID trackId, Long dvsId) {
        log.info("Dvs get web url is started : trackId - {} , dvsId - {}", trackId, dvsId);
        try {
            var webUrl = dvsClient.getDetails(trackId, dvsId).getWebUrl();
            log.info("Dvs get web url was finished : trackId - {} , webUrl - {}", trackId,
                    webUrl);
            return Optional.ofNullable(webUrl);
        } catch (FeignException e) {
            log.error("Dvs get web url was failed : trackId - {} , exception - {}",
                    trackId, e);
            return Optional.empty();
        }
    }
}
