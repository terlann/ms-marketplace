package az.kapitalbank.marketplace.service;

import java.util.Optional;
import java.util.UUID;

import az.kapitalbank.marketplace.client.dvs.DvsClient;
import az.kapitalbank.marketplace.client.dvs.model.DvsCreateOrderRequest;
import az.kapitalbank.marketplace.client.dvs.model.DvsCreateOrderResponse;
import az.kapitalbank.marketplace.client.dvs.model.DvsGetDetailsResponse;
import az.kapitalbank.marketplace.exception.FeignClientException;
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

    public Optional<DvsCreateOrderResponse> createOrder(DvsCreateOrderRequest request, UUID trackId) {
        log.info("Verification service create order start... track_id - [{}]", trackId);
        try {
            DvsCreateOrderResponse dvsCreateOrderResponse = dvsClient.createOrder(request);
            log.error("Verification service create order finish... track_id - [{}]", trackId);
            return Optional.of(dvsCreateOrderResponse);
        } catch (FeignClientException f) {
            log.error("Verification service create order error... track_id - [{}]", trackId);
            return Optional.empty();
        }
    }

    public Optional<DvsGetDetailsResponse> getDetails(UUID trackId, Long dvsId) {
        log.info("Verification service get details start... track_id - [{}],dvs_id - [{}]",
                trackId,
                dvsId);
        try {
            DvsGetDetailsResponse dvsGetDetailsResponse = dvsClient.getDetails(trackId, dvsId);
            log.info("Verification service get details finish... track_id - [{}]", trackId);
            return Optional.of(dvsGetDetailsResponse);
        } catch (FeignClientException f) {
            log.error("Verification service get details error... track_id - [{}]", trackId);
            return Optional.empty();
        }
    }
}
