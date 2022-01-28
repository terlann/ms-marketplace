package az.kapitalbank.marketplace.service;

import java.util.UUID;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.checker.CheckerClient;
import az.kapitalbank.marketplace.dto.response.BalanceResponseDto;
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
    AtlasClient atlasClient;
    CustomerRepository customerRepository;

    public void checkPin(String pin) {
        log.info("check customer pin-code service start... pin_code - [{}]", pin);
        try {
            boolean result = checkerClient.checkPinCode(pin);
            log.info("check customer pin-code service.pin_code - [{}], Response - {}", pin, result);
            if (!result)
                throw new PinCodeInCorrectException("this pin code is incorrect");
        } catch (FeignException f) {
            log.error("check customer pin-code service.pin_code - [{}], FeignException - {}", pin, f.getMessage());
        }
        log.info("check customer pin-code service finish.. pin_code - [{}]", pin);
    }


    public BalanceResponseDto getBalance(String umicoUserId, UUID customerId) {
// TODO learn what some fields means in BalanceResponseDto
        var cardUUID = customerRepository.findById(customerId).get().getCardUUID();
        var balanceResponse = atlasClient.balance(cardUUID);
//TODO balanceResponse map to BalanceResponseDto and return
        return null;
    }
}
