package az.kapitalbank.marketplace.controller;

import javax.validation.Valid;

import az.kapitalbank.marketplace.service.CheckCustomerService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/marketplace/check")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CheckCustomerController {

    CheckCustomerService service;

    @GetMapping("/{pinCode}/customer")
    public ResponseEntity<?> checkPinCode(@Valid @PathVariable("pinCode") String pinCode) {
        return service.checkPinCode(pinCode);
    }

}
