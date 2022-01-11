package az.kapitalbank.marketplace.service;

import az.kapitalbank.marketplace.dto.request.DeliveryProductRequestDto;
import org.springframework.http.ResponseEntity;

public interface DeliveryService {

    ResponseEntity<Object> deliveryProducts(DeliveryProductRequestDto request);
}
