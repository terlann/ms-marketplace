package az.kapitalbank.marketplace.service;

import java.time.LocalDate;
import java.util.UUID;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.checker.CheckerClient;
import az.kapitalbank.marketplace.dto.response.BalanceResponseDto;
import az.kapitalbank.marketplace.exception.PinNotFoundException;
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
                throw new PinNotFoundException(pin);
        } catch (FeignException f) {
            log.error("check customer pin-code service.pin_code - [{}], FeignException - {}", pin, f.getMessage());
        }
        log.info("check customer pin-code service finish.. pin_code - [{}]", pin);
    }


    public BalanceResponseDto getBalance(String umicoUserId, UUID customerId) {
        var customerEntity = customerRepository.findById(customerId)
                .orElseThrow(() -> new RuntimeException("Customer not found"));
        if (!customerEntity.getUmicoUserId().equals(umicoUserId)) {
            throw new RuntimeException("Umico user not found");
        }
        var cardUUID = customerEntity.getCardUUID();
        var balanceResponse = atlasClient.balance(cardUUID);
        //TODO loan utilized and loan limit must be fixed
        return BalanceResponseDto.builder()
                .availableBalance(balanceResponse.getAvailableBalance())
                .loanEndDate(LocalDate.of(2025, 02, 15))
                .loanUtilized(balanceResponse.getOverdraftLimit().subtract(balanceResponse.getAvailableBalance()))
                .loanLimit(balanceResponse.getOverdraftLimit())
                .build();
    }
}
