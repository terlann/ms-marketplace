package az.kapitalbank.marketplace.controller;

import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.dto.request.PurchaseRequestDto;
import az.kapitalbank.marketplace.dto.request.ReverseRequestDto;
import az.kapitalbank.marketplace.dto.request.TelesalesResultRequestDto;
import az.kapitalbank.marketplace.dto.response.CheckOrderResponseDto;
import az.kapitalbank.marketplace.dto.response.CreateOrderResponse;
import az.kapitalbank.marketplace.dto.response.PurchaseResponseDto;
import az.kapitalbank.marketplace.service.OrderService;
import az.kapitalbank.marketplace.service.ScoringService;
import java.util.List;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    ScoringService scoringService;

    @PostMapping
    public ResponseEntity<CreateOrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequestDto request) {
        return new ResponseEntity<>(service.createOrder(request), HttpStatus.CREATED);
    }

    @PostMapping("/check/{telesales-order-id}")
    public ResponseEntity<CheckOrderResponseDto> checkOrder(
            @NotBlank @PathVariable("telesales-order-id")
                    String telesalesOrderId) {
        return ResponseEntity.ok(service.checkOrder(telesalesOrderId));
    }

    @PostMapping("/telesales/result")
    public ResponseEntity<Void> telesalesResult(
            @Valid @RequestBody TelesalesResultRequestDto request) {
        scoringService.telesalesResult(request);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping("/purchase")
    public ResponseEntity<List<PurchaseResponseDto>> purchase(
            @Valid @RequestBody PurchaseRequestDto request) {
        return ResponseEntity.ok(service.purchase(request));
    }

    @PostMapping("/reverse")
    public ResponseEntity<PurchaseResponseDto> reverse(
            @Valid @RequestBody ReverseRequestDto request) {
        return ResponseEntity.ok(service.reverse(request));
    }
}
