package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constants.TestConstants.CARD_UID;
import static az.kapitalbank.marketplace.constants.TestConstants.MOBILE_NUMBER;
import static az.kapitalbank.marketplace.constants.TestConstants.TRACK_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.kapitalbank.marketplace.client.common.CommonClient;
import az.kapitalbank.marketplace.client.common.model.request.SendSmsRequest;
import az.kapitalbank.marketplace.client.common.model.response.SendSmsResponse;
import az.kapitalbank.marketplace.config.SmsProperties;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import feign.FeignException;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;


@ExtendWith(MockitoExtension.class)
class SmsServiceTest {
    @Mock
    CommonClient commonClient;
    @Spy
    SmsProperties smsProperties;
    @Mock
    OtpService otpService;

    @InjectMocks
    SmsService smsService;

    @BeforeEach
    void start() {
        ReflectionTestUtils.setField(this.smsProperties, "text",
                Map.of("complete-scoring",
                        "{contractNumber} sayli  sorgunuz uzre {amount} AZN kredit xetti"
                                + " tesdiqlendi. Minimal odenish meblegi her ayin 1-10 "
                                + "araligindadir. Musteri kodu - {cif}",
                        "pre-purchase",
                        "Marketplace kredit xetti uzre {amount} AZN odenis ugurla tamamlandi."));
    }

    @Test
    void sendPreApproveSms_Success() {
        var operationEntity = OperationEntity.builder()
                .id(UUID.fromString(TRACK_ID.getValue()))
                .mobileNumber(MOBILE_NUMBER.getValue())
                .build();
        var sendSmsRequest = SendSmsRequest.builder()
                .body(any())
                .phoneNumber(operationEntity.getMobileNumber())
                .build();
        when(commonClient.sendSms(sendSmsRequest)).thenReturn(
                new SendSmsResponse(UUID.fromString(TRACK_ID.getValue())));
        smsService.sendPreapproveSms(operationEntity);
        verify(commonClient).sendSms(sendSmsRequest);
    }

    @Test
    void sendPreApproveSms_Exception() {
        var operationEntity = OperationEntity.builder()
                .id(UUID.fromString(TRACK_ID.getValue()))
                .mobileNumber(MOBILE_NUMBER.getValue())
                .cif("1234567")
                .contractNumber("BUMM123456789")
                .build();

        doThrow(FeignException.class).when(commonClient)
                .sendSms(new SendSmsRequest(any(), MOBILE_NUMBER.getValue()));
        smsService.sendPreapproveSms(operationEntity);
        verify(commonClient).sendSms(new SendSmsRequest(any(), MOBILE_NUMBER.getValue()));
    }

    @Test
    void sendCompleteScoringSms_Success() {
        var operationEntity = OperationEntity.builder()
                .id(UUID.fromString(TRACK_ID.getValue()))
                .mobileNumber(MOBILE_NUMBER.getValue())
                .scoredAmount(BigDecimal.valueOf(55))
                .contractNumber("BUMM123456789")
                .cif("1234567")
                .build();
        var sendSmsRequest = SendSmsRequest.builder()
                .body("BUMM123456789 sayli  sorgunuz uzre 55 AZN kredit xetti tesdiqlendi. Minimal"
                        + " odenish meblegi her ayin 1-10 araligindadir. Musteri kodu - 1234567")
                .phoneNumber(operationEntity.getMobileNumber())
                .build();

        when(commonClient.sendSms(sendSmsRequest)).thenReturn(
                new SendSmsResponse(UUID.fromString(TRACK_ID.getValue())));
        smsService.sendCompleteScoringSms(operationEntity);
        verify(commonClient).sendSms(sendSmsRequest);
    }

    @Test
    void sendPrePurchaseSms_Success() {
        var customer = CustomerEntity.builder()
                .cardId(CARD_UID.getValue())
                .build();
        var operationEntity = OperationEntity.builder()
                .id(UUID.fromString(TRACK_ID.getValue()))
                .mobileNumber(MOBILE_NUMBER.getValue())
                .totalAmount(BigDecimal.valueOf(55))
                .commission(BigDecimal.valueOf(0))
                .customer(customer)
                .build();
        var sendSmsRequest = SendSmsRequest.builder()
                .body("Marketplace kredit xetti uzre 55 AZN odenis ugurla tamamlandi.")
                .phoneNumber(operationEntity.getMobileNumber())
                .build();
        when(commonClient.sendSms(sendSmsRequest)).thenReturn(
                new SendSmsResponse(UUID.fromString(TRACK_ID.getValue())));
        when(otpService.getCardLinkedMobileNumber(operationEntity.getCustomer().getCardId(),
                operationEntity.getId()))
                .thenReturn(MOBILE_NUMBER.getValue());
        smsService.sendPrePurchaseSms(operationEntity);
        verify(commonClient).sendSms(sendSmsRequest);
    }

    @Test
    void sendPendingSms_Success() {
        var operationEntity = OperationEntity.builder()
                .id(UUID.fromString(TRACK_ID.getValue()))
                .mobileNumber(MOBILE_NUMBER.getValue())
                .build();
        var sendSmsRequest = SendSmsRequest.builder()
                .body(any())
                .phoneNumber(operationEntity.getMobileNumber())
                .build();
        when(commonClient.sendSms(sendSmsRequest)).thenReturn(
                new SendSmsResponse(UUID.fromString(TRACK_ID.getValue())));
        smsService.sendPendingSms(operationEntity);
        verify(commonClient).sendSms(sendSmsRequest);
    }
}
