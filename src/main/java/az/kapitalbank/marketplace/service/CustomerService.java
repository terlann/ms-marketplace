package az.kapitalbank.marketplace.service;

import java.math.BigDecimal;
import java.util.UUID;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.integration.IamasClient;
import az.kapitalbank.marketplace.client.integration.model.IamasResponse;
import az.kapitalbank.marketplace.constant.AccountStatus;
import az.kapitalbank.marketplace.constant.ResultType;
import az.kapitalbank.marketplace.dto.response.BalanceResponseDto;
import az.kapitalbank.marketplace.exception.CustomerNotFoundException;
import az.kapitalbank.marketplace.exception.PersonNotFoundException;
import az.kapitalbank.marketplace.exception.UmicoUserNotFoundException;
import az.kapitalbank.marketplace.repository.CustomerRepository;
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

    AtlasClient atlasClient;
    CustomerRepository customerRepository;
    IamasClient iamasClient;

    public void checkPerson(String pin) {
        log.info("Checking starts by pin in IAMAS. Pin - {} ", pin);
        var iamasResponse = iamasClient.findPersonByPin(pin)
                .stream()
                .filter(IamasResponse::isActive)
                .findFirst();

        if (iamasResponse.isEmpty())
            throw new PersonNotFoundException("pin - " + pin);
        log.info("Pin found in IAMAS. Pin - {} ", pin);
    }

    public BalanceResponseDto getBalance(String umicoUserId, UUID customerId) {
        var customerEntity = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("customerId - " + customerId));

        if (!customerEntity.getUmicoUserId().equals(umicoUserId)) {
            throw new UmicoUserNotFoundException("umicoUserId - " + umicoUserId);
        }
        var cardUUID = customerEntity.getCardId();
        var cardDetailResponse = atlasClient.findCardByUID(cardUUID, ResultType.ACCOUNT);

        var primaryAccount = cardDetailResponse.getAccounts()
                .stream()
                .filter(x -> x.getStatus() == AccountStatus.OPEN_PRIMARY)
                .findFirst();
        if (primaryAccount.isEmpty()) {
            log.error("Account not found in open primary status.cardId - {}", cardUUID);
            return BalanceResponseDto.builder()
                    .loanUtilized(BigDecimal.ZERO)
                    .availableBalance(BigDecimal.ZERO)
                    .loanLimit(BigDecimal.ZERO)
                    .cardExpiryDate(cardDetailResponse.getExpiryDate())
                    .build();
        }
        var loanLimit = primaryAccount.get().getOverdraftLimit();
        var availableBalance = primaryAccount.get().getAvailableBalance();
        return BalanceResponseDto.builder()
                .loanUtilized(loanLimit.subtract(availableBalance))
                .availableBalance(availableBalance)
                .loanLimit(loanLimit)
                .cardExpiryDate(cardDetailResponse.getExpiryDate())
                .build();
    }
}
