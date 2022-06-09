package az.kapitalbank.marketplace.service;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.integration.IamasClient;
import az.kapitalbank.marketplace.client.integration.model.IamasResponse;
import az.kapitalbank.marketplace.constant.AccountStatus;
import az.kapitalbank.marketplace.constant.Error;
import az.kapitalbank.marketplace.constant.ResultType;
import az.kapitalbank.marketplace.dto.response.BalanceResponseDto;
import az.kapitalbank.marketplace.exception.CommonException;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import feign.FeignException;
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
                throw new CommonException(Error.PERSON_NOT_FOUND,
                        "Person not found in IAMAS. pin - " + pin);
            }
        } catch (FeignException e) {
            log.error("Check person was failed : pin - {}, exception - {}", pin, e);
            throw new CommonException(Error.PERSON_NOT_FOUND,
                    "Person not found in IAMAS. pin - " + pin);
        }
        log.info("Check person was finished : pin - {} ", pin);
    }

    public BalanceResponseDto getBalance(String umicoUserId, UUID customerId) {
        var customerEntity = customerRepository.findById(customerId)
                .orElseThrow(() -> new CommonException(Error.CUSTOMER_NOT_FOUND,
                        "Customer not found. CustomerId - " + customerId));
        if (!customerEntity.getUmicoUserId().equals(umicoUserId)) {
            throw new CommonException(Error.UMICO_USER_NOT_FOUND,
                    "Umico user not found. UmicoUserId - " + umicoUserId);
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
                    .cardExpiryDate(cardDetailResponse.getExpiryDate()).build();
        }
        var loanLimit = primaryAccount.get().getOverdraftLimit();
        var availableBalance = primaryAccount.get().getAvailableBalance();
        var lastAvailableBalance = availableBalance.subtract(customerEntity.getLastTempAmount());
        return BalanceResponseDto.builder()
                .loanUtilized(loanLimit.subtract(lastAvailableBalance))
                .availableBalance(lastAvailableBalance)
                .loanLimit(loanLimit)
                .cardExpiryDate(cardDetailResponse.getExpiryDate())
                .build();
    }
}
