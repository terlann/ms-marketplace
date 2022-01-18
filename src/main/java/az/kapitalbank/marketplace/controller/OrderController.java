package az.kapitalbank.marketplace.controller;

import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.service.OrderService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/marketplace/order")
@Tag(name = "order", description = "the order API")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class OrderController {

    OrderService service;

    @PostMapping
    public ResponseEntity<?> createOrder(@Valid @RequestBody CreateOrderRequestDto request) {
        return ResponseEntity.ok(service.createOrder(request));
    }

    @DeleteMapping(params = "trackId")
    public ResponseEntity<?> deleteOrder(@Valid @RequestParam String trackId) {
        return ResponseEntity.ok(service.deleteOrder(trackId));
    }
}
