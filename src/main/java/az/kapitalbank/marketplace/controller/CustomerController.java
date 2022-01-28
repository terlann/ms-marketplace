package az.kapitalbank.marketplace.controller;

import javax.validation.Valid;
import java.util.UUID;

import az.kapitalbank.marketplace.dto.response.BalanceResponseDto;
import az.kapitalbank.marketplace.service.CustomerService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/customers")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerController {

    CustomerService customerService;

    @GetMapping("/{pin}")
    public ResponseEntity<Void> checkPin(@Valid @PathVariable String pin) {
        customerService.checkPin(pin);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping("/balance")
    public ResponseEntity<BalanceResponseDto> getBalance(@Valid
                                                         @RequestParam("umico_user_id") String umicoUserId,
                                                         @RequestParam("customer_id") UUID customerId) {
        return ResponseEntity.ok(customerService.getBalance(umicoUserId, customerId));
    }
}
