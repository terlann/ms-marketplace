package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constants.ConstantObject.getCardDetailResponse;
import static az.kapitalbank.marketplace.constants.TestConstants.CARD_UID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.model.response.AccountResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.CardDetailResponse;
import az.kapitalbank.marketplace.client.integration.IamasClient;
import az.kapitalbank.marketplace.client.integration.model.IamasPerson;
import az.kapitalbank.marketplace.client.integration.model.IamasResponse;
import az.kapitalbank.marketplace.constant.AccountStatus;
import az.kapitalbank.marketplace.constant.ResultType;
import az.kapitalbank.marketplace.dto.response.BalanceResponseDto;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private AtlasClient atlasClient;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private IamasClient iamasClient;
    @InjectMocks
    CustomerService customerService;

    @Test
    void checkPerson() {
        var pin = "AA11B22";
        var iamasPerson = IamasPerson.builder()
                .active(true)
                .build();
        var iamasResponse = IamasResponse.builder()
                .active(true)
                .documentNumber("")
                .pin("")
                .personAz(iamasPerson)
                .build();
        var iamasResponses = List.of(iamasResponse);
        when(iamasClient.findPersonByPin(pin)).thenReturn(iamasResponses);
        customerService.checkPerson(pin);
        verify(iamasClient).findPersonByPin(pin);
    }

    @Test
    void getBalance_Success() {
        var umicoUserId = "9eb6e760-9a25-11ec-b909-0242ac120002";
        var customerId = UUID.fromString("98f12a70-9a25-11ec-b909-0242ac120002");
        var cardId = "81E8CBF84249D915E0530100007FF443";
        var customerEntity = CustomerEntity.builder()
                .umicoUserId(umicoUserId)
                .isAgreement(true)
                .cardId(cardId)
                .build();
        var accountResponse = AccountResponse.builder()
                .availableBalance(BigDecimal.valueOf(500))
                .overdraftLimit(BigDecimal.valueOf(1000))
                .status(AccountStatus.OPEN_PRIMARY)
                .build();
        var cardDetailResponse = CardDetailResponse.builder()
                .expiryDate(LocalDateTime.now())
                .accounts(List.of(accountResponse))
                .build();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customerEntity));
        when(atlasClient.findCardByUid(cardId, ResultType.ACCOUNT)).thenReturn(cardDetailResponse);
        var expected = BalanceResponseDto.builder()
                .cardExpiryDate(cardDetailResponse.getExpiryDate())
                .loanLimit(BigDecimal.valueOf(1000))
                .loanUtilized(BigDecimal.valueOf(500))
                .availableBalance(BigDecimal.valueOf(500))
                .build();
        var actual = customerService.getBalance(umicoUserId, customerId);
        assertEquals(expected, actual);

    }

    @Test
    void getBalance_when_primaryAccountIsEmpty() {
        var umicoUserId = "9eb6e760-9a25-11ec-b909-0242ac120002";
        var customerId = UUID.fromString("98f12a70-9a25-11ec-b909-0242ac120002");
        var cardId = "81E8CBF84249D915E0530100007FF443";
        var customerEntity = CustomerEntity.builder()
                .umicoUserId(umicoUserId)
                .isAgreement(true)
                .cardId(cardId)
                .build();

        var cardDetailResponse = CardDetailResponse.builder()
                .expiryDate(LocalDateTime.now())
                .accounts(new ArrayList<>())
                .build();
        when(customerRepository.findById(customerId)).thenReturn(Optional.of(customerEntity));
        when(atlasClient.findCardByUid(cardId, ResultType.ACCOUNT)).thenReturn(cardDetailResponse);
        var expected = BalanceResponseDto.builder()
                .cardExpiryDate(cardDetailResponse.getExpiryDate())
                .loanLimit(BigDecimal.ZERO)
                .loanUtilized(BigDecimal.ZERO)
                .availableBalance(BigDecimal.ZERO)
                .build();
        var actual = customerService.getBalance(umicoUserId, customerId);
        assertEquals(expected, actual);
    }

    @Test
    void getLoanLimit_Success() {
        when(atlasClient.findCardByUid(CARD_UID.getValue(), ResultType.ACCOUNT)).thenReturn(
                getCardDetailResponse());
    }

    @Test
    void getLoanLimit_AtlasException() {
        when(atlasClient.findCardByUid(CARD_UID.getValue(), ResultType.ACCOUNT)).thenThrow(
                new RuntimeException());
    }

    @Test
    void getLoanLimit_NotPrimaryAccount() {
        when(atlasClient.findCardByUid(CARD_UID.getValue(), ResultType.ACCOUNT)).thenReturn(
                getNoPrimaryAccountCardDetailResponse());
    }

    CardDetailResponse getNoPrimaryAccountCardDetailResponse() {
        return CardDetailResponse.builder()
                .accounts(List.of())
                .build();
    }

}
