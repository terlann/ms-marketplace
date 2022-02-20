package az.kapitalbank.marketplace.service;

import java.util.UUID;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.externalinteg.ExternalIntegrationClient;
import az.kapitalbank.marketplace.client.externalinteg.model.IamasResponse;
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
        var cardId = customerEntity.getCardId();
        var balanceResponse = atlasClient.balance(cardId);
        //TODO just some fields isn't exact in response
        return BalanceResponseDto.builder()
                .cardExpiryDate(null)
                .loanUtilized(null)
                .availableBalance(balanceResponse.getAvailableBalance())
                .loanLimit(balanceResponse.getOverdraftLimit())
                .build();
    }
}
