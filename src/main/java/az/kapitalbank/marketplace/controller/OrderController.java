package az.kapitalbank.marketplace.controller;

import javax.validation.Valid;
import java.util.UUID;

import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.dto.request.PurchaseRequestDto;
import az.kapitalbank.marketplace.dto.request.ReverseRequestDto;
import az.kapitalbank.marketplace.dto.response.CreateOrderResponse;
import az.kapitalbank.marketplace.service.OrderService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class OrderController {

    OrderService service;

    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(@Valid @RequestBody CreateOrderRequestDto request) {
        return new ResponseEntity<>(service.createOrder(request), HttpStatus.CREATED);
    }

    @DeleteMapping("/{trackId}")
    public ResponseEntity<Void> deleteOrder(@Valid @PathVariable UUID trackId) {
        service.deleteOrder(trackId);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/purchase")
    public ResponseEntity<Void> purchase(@Valid @RequestBody PurchaseRequestDto request) {
        service.purchase(request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/reverse")
    public ResponseEntity<Void> reverse(@Valid @RequestBody ReverseRequestDto request) {
        service.reverse(request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
