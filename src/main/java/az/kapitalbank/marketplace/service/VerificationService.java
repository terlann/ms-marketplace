package az.kapitalbank.marketplace.service;

import az.kapitalbank.marketplace.client.dvs.DvsClient;
import az.kapitalbank.marketplace.client.dvs.exception.DvsClientException;
import az.kapitalbank.marketplace.client.optimus.OptimusClient;
import az.kapitalbank.marketplace.repository.OperationRepository;
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
    UmicoService umicoService;
    OptimusClient optimusClient;
    ScoringService scoringService;
    TelesalesService telesalesService;
    OperationRepository operationRepository;

    public Optional<String> getDvsUrl(UUID trackId, Long dvsId) {
        log.info("Dvs get web url is started : trackId - {} , dvsId - {}", trackId, dvsId);
        try {
            var webUrl = dvsClient.getDetails(trackId, dvsId).getWebUrl();
            log.info("Dvs get web url was finished : trackId - {} , webUrl - {}", trackId,
                    webUrl);
            return Optional.ofNullable(webUrl);
        } catch (DvsClientException e) {
            log.error("Dvs get web url was failed : trackId - {} , DvsClientException - {}",
                    trackId, e);
            return Optional.empty();
        }
    }
}
