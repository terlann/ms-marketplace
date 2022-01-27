package az.kapitalbank.marketplace.service;

import az.kapitalbank.marketplace.client.checker.CheckerClient;
import az.kapitalbank.marketplace.exception.PinCodeInCorrectException;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import feign.FeignException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerService {

    CheckerClient checkerClient;
    CustomerRepository customerRepository;

    public void checkPinCode(String pinCode) {
        log.info("check customer pin-code service start... pin_code - [{}]", pinCode);
        try {
            boolean result = checkerClient.checkPinCode(pinCode);
            log.info("check customer pin-code service.pin_code - [{}], Response - {}", pinCode, result);
            if (!result)
                throw new PinCodeInCorrectException("this pin code is incorrect");
        } catch (FeignException f) {
            log.error("check customer pin-code service.pin_code - [{}], FeignException - {}", pinCode, f.getMessage());
        }
        log.info("check customer pin-code service finish.. pin_code - [{}]", pinCode);
    }
}
