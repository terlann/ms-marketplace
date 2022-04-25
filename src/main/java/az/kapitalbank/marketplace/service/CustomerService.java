package az.kapitalbank.marketplace.service;

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
import feign.FeignException;
import java.math.BigDecimal;
import java.util.UUID;
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
        log.info("Check person is started : pin - {} ", pin);
        try {
            var iamasResponse = iamasClient.findPersonByPin(pin)
                    .stream()
                    .filter(IamasResponse::isActive)
                    .findFirst();
            if (iamasResponse.isEmpty()) {
                throw new PersonNotFoundException("pin - " + pin);
            }
        } catch (FeignException e) {
            log.error("Check person was failed : pin - {}, exception - {}", pin, e);
            throw new PersonNotFoundException("pin - " + pin);
        }
        log.info("Check person was finished : pin - {} ", pin);
    }

    public BalanceResponseDto getBalance(String umicoUserId, UUID customerId) {
        var customerEntity = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("customerId - " + customerId));
        if (!customerEntity.getUmicoUserId().equals(umicoUserId)) {
            throw new UmicoUserNotFoundException("umicoUserId - " + umicoUserId);
        }
        var cardId = customerEntity.getCardId();
        var cardDetailResponse = atlasClient.findCardByUid(cardId, ResultType.ACCOUNT);

        var primaryAccount = cardDetailResponse.getAccounts()
                .stream()
                .filter(x -> x.getStatus() == AccountStatus.OPEN_PRIMARY)
                .findFirst();
        if (primaryAccount.isEmpty()) {
            log.error("Open primary account not found : cardId - {}, primaryAccount - {}", cardId,
                    primaryAccount);
            return BalanceResponseDto.builder()
                    .loanUtilized(BigDecimal.ZERO)
                    .availableBalance(BigDecimal.ZERO)
                    .loanLimit(BigDecimal.ZERO)
                    .cardExpiryDate(cardDetailResponse.getExpiryDate()).build();
        }
        var loanLimit = primaryAccount.get().getOverdraftLimit();
        var availableBalance = primaryAccount.get().getAvailableBalance();
        return BalanceResponseDto.builder()
                .loanUtilized(loanLimit.subtract(availableBalance))
                .availableBalance(availableBalance.subtract(customerEntity.getLastTempAmount()))
                .loanLimit(loanLimit)
                .cardExpiryDate(cardDetailResponse.getExpiryDate())
                .build();
    }
}
