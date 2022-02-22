package az.kapitalbank.marketplace.service;

import java.util.UUID;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.externalinteg.ExternalIntegrationClient;
import az.kapitalbank.marketplace.client.externalinteg.model.IamasResponse;
import az.kapitalbank.marketplace.constant.AccountStatus;
import az.kapitalbank.marketplace.constants.ResultType;
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
    ExternalIntegrationClient externalIntegrationClient;

    public void checkPerson(String pin) {
        log.info("Checking starts by pin in IAMAS. Pin - {} ", pin);
        var iamasResponse = externalIntegrationClient.getData(pin)
                .stream()
                .filter(IamasResponse::isActive)
                .findFirst();

        if (iamasResponse.isEmpty())
            throw new PersonNotFoundException("Pin - " + pin);
        log.info("Pin found in IAMAS. Pin - {} ", pin);
    }

    public BalanceResponseDto getBalance(String umicoUserId, UUID customerId) {
        var customerEntity = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("customerId - " + customerId));

        if (!customerEntity.getUmicoUserId().equals(umicoUserId)) {
            throw new UmicoUserNotFoundException(umicoUserId);
        }
        var cardUUID = customerEntity.getCardId();
        var cardDetailResponse = atlasClient.findCardByUID(cardUUID, ResultType.ACCOUNT);

        var accountResponseList = cardDetailResponse.getAccounts()
                .stream()
                .filter(x -> x.getStatus() == AccountStatus.OPEN_PRIMARY)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Open Primary Account not Found in Account Response"));

        var loanLimit = accountResponseList.getOverdraftLimit();
        var availableBalance = accountResponseList.getAvailableBalance();
        return BalanceResponseDto.builder()
                .loanUtilized(loanLimit.subtract(availableBalance))
                .availableBalance(availableBalance)
                .loanLimit(loanLimit)
                .cardExpiryDate(cardDetailResponse.getExpiryDate())
                .build();
    }
}
