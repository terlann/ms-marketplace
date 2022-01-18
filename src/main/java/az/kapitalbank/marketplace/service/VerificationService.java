package az.kapitalbank.marketplace.service;

import java.util.Optional;

import az.kapitalbank.marketplace.client.dvs.model.DvsCreateOrderRequest;
import az.kapitalbank.marketplace.client.dvs.model.DvsCreateOrderResponse;
import az.kapitalbank.marketplace.client.dvs.model.DvsGetDetailsResponse;

public interface VerificationService {

    Optional<DvsGetDetailsResponse> getDetails(String trackId, String orderId, String dvsId);

    Optional<DvsCreateOrderResponse> createOrder(DvsCreateOrderRequest request, String trackId);
}
