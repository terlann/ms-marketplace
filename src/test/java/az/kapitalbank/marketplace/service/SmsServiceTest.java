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
        ReflectionTestUtils.setField(this.smsProperties, "values",
                Map.of("complete-scoring", "Kredit sorgunuz tesdiqlendi.Kredit xetti %s manat.",
                        "pre-purchase", "%s manat bloklanib."));
    }

    @Test
    void smsService_sendSmsPreapprove_Success() {
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
        smsService.sendSmsPreapprove(operationEntity);
        verify(commonClient).sendSms(sendSmsRequest);
    }

    @Test
    void smsService_sendSmsCompleteScoring_Success() {

        var operationEntity = OperationEntity.builder()
                .id(UUID.fromString(TRACK_ID.getValue()))
                .mobileNumber(MOBILE_NUMBER.getValue())
                .scoredAmount(BigDecimal.valueOf(55))
                .build();
        var sendSmsRequest = SendSmsRequest.builder()
                .body("Kredit sorgunuz tesdiqlendi.Kredit xetti 55 manat.")
                .phoneNumber(operationEntity.getMobileNumber())
                .build();

        when(commonClient.sendSms(sendSmsRequest)).thenReturn(
                new SendSmsResponse(UUID.fromString(TRACK_ID.getValue())));
        smsService.sendSmsCompleteScoring(operationEntity);
        verify(commonClient).sendSms(sendSmsRequest);
    }

    @Test
    void smsService_sendSmsPrePurchase_Success() {
        var customer = CustomerEntity.builder()
                .cardId(CARD_UID.getValue())
                .build();
        var operationEntity = OperationEntity.builder()
                .id(UUID.fromString(TRACK_ID.getValue()))
                .mobileNumber(MOBILE_NUMBER.getValue())
                .totalAmount(BigDecimal.valueOf(55))
                .customer(customer)
                .build();
        var sendSmsRequest = SendSmsRequest.builder()
                .body("55 manat bloklanib.")
                .phoneNumber(operationEntity.getMobileNumber())
                .build();
        when(commonClient.sendSms(sendSmsRequest)).thenReturn(
                new SendSmsResponse(UUID.fromString(TRACK_ID.getValue())));
        when(otpService.getCardLinkedMobileNumber(operationEntity.getCustomer().getCardId()))
                .thenReturn(MOBILE_NUMBER.getValue());
        smsService.sendSmsPrePurchase(operationEntity);
        verify(commonClient).sendSms(sendSmsRequest);
    }

    @Test
    void smsService_sendSmsPending_Success() {
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
        smsService.sendSmsPending(operationEntity);
        verify(commonClient).sendSms(sendSmsRequest);
    }

    @Test
    void sendSms_Exception() {
        var operationEntity = OperationEntity.builder()
                .id(UUID.fromString(TRACK_ID.getValue()))
                .mobileNumber(MOBILE_NUMBER.getValue())
                .build();
        doThrow(FeignException.class).when(commonClient)
                .sendSms(new SendSmsRequest(any(), MOBILE_NUMBER.getValue()));
        smsService.sendSmsPreapprove(operationEntity);
        verify(commonClient).sendSms(new SendSmsRequest(any(), MOBILE_NUMBER.getValue()));
    }
}
