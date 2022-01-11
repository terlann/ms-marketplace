package az.kapitalbank.marketplace.controller;

import javax.validation.Valid;

import az.kapitalbank.marketplace.dto.request.DeliveryProductRequestDto;
import az.kapitalbank.marketplace.service.DeliveryService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/marketplace/delivery")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class DeliveryController {

    DeliveryService service;

    @PostMapping
    public ResponseEntity<?> deliveryProducts(@Valid @RequestBody DeliveryProductRequestDto request) {
        return service.deliveryProducts(request);
    }
}
