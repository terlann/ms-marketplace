package az.kapitalbank.marketplace.service.impl;

import az.kapitalbank.marketplace.client.dvs.DvsClient;
import az.kapitalbank.marketplace.client.dvs.model.DvsCreateOrderRequest;
import az.kapitalbank.marketplace.client.dvs.model.DvsCreateOrderResponse;
import az.kapitalbank.marketplace.client.dvs.model.DvsGetDetailsResponse;
import az.kapitalbank.marketplace.exception.models.FeignClientException;
import az.kapitalbank.marketplace.service.VerificationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;


@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true,level = AccessLevel.PRIVATE)
public class VerificationServiceImpl implements VerificationService {

    DvsClient dvsClient;

    @Override
    public Optional<DvsGetDetailsResponse> getDetails(String trackId, String orderId, String dvsId) {
        log.info("verification service get details start... track_id - [{}],order_id - [{}],dvs_id - [{}]",
                trackId,
                orderId,
                dvsId);
        try {
            DvsGetDetailsResponse dvsGetDetailsResponse = dvsClient.getDetails(orderId, dvsId);
            return Optional.of(dvsGetDetailsResponse);
        } catch (FeignClientException f) {
            log.error("verification service get details finish... track_id - [{}]", trackId);
            return Optional.empty();
        }
    }

    @Override
    public Optional<DvsCreateOrderResponse> createOrder(DvsCreateOrderRequest request, String trackId) {
        log.info("verification service create order start... track_id - [{}]", trackId);
        try {
            DvsCreateOrderResponse dvsCreateOrderResponse = dvsClient.createOrder(request);
            return Optional.of(dvsCreateOrderResponse);
        } catch (FeignClientException f) {
            log.error("verification service create order finish... track_id - [{}]", trackId);
            return Optional.empty();
        }
    }
}
