package az.kapitalbank.marketplace.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.exception.AtlasClientException;
import az.kapitalbank.marketplace.client.atlas.model.request.PurchaseRequest;
import az.kapitalbank.marketplace.client.atlas.model.response.PurchaseResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.Subscription;
import az.kapitalbank.marketplace.client.atlas.model.response.SubscriptionResponse;
import az.kapitalbank.marketplace.client.otp.OtpClient;
import az.kapitalbank.marketplace.client.otp.exception.OtpClientException;
import az.kapitalbank.marketplace.client.otp.model.ChannelRequest;
import az.kapitalbank.marketplace.client.otp.model.OtpVerifyRequest;
import az.kapitalbank.marketplace.client.otp.model.OtpVerifyResponse;
import az.kapitalbank.marketplace.client.otp.model.SendOtpRequest;
import az.kapitalbank.marketplace.client.otp.model.SendOtpResponse;
import az.kapitalbank.marketplace.dto.request.OtpVerifyRequestDto;
import az.kapitalbank.marketplace.dto.request.SendOtpRequestDto;
import az.kapitalbank.marketplace.dto.response.OtpVerifyResponseDto;
import az.kapitalbank.marketplace.dto.response.SendOtpResponseDto;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.repository.OperationRepository;
import az.kapitalbank.marketplace.repository.OrderRepository;
import lombok.experimental.NonFinal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;

import static az.kapitalbank.marketplace.constants.TestConstants.CARD_UID;
import static az.kapitalbank.marketplace.constants.TestConstants.RRN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {
    @Mock
    AtlasClient atlasClient;

    @Mock
    OtpClient otpClient;

    @Mock
    OperationRepository operationRepository;

    @Mock
    OrderRepository orderRepository;

    @InjectMocks
    OtpService otpService;


    @NonFinal
    @Value("${purchase.terminal-name}")
    String terminalName;

    @Test
    void send_Success() {
        //GIVEN
        var trackId = UUID.fromString("3a30a65a-9bec-11ec-b909-0242ac120002");
        var request = SendOtpRequestDto.builder()
                .trackId(trackId).build();
        String cardConnectedNumber = "+994513601019";
        SendOtpRequest sendOtpRequest = SendOtpRequest.builder()
                .phoneNumber(cardConnectedNumber)
                .definitionId(UUID.fromString("00608fa-9bae-11ec-b909-0242ac120002"))
                .channel(ChannelRequest.builder()
                        .channel("Umico Marketplace")
                        .build())
                .build();
        SendOtpResponse sendOtpResponse = SendOtpResponse.builder()
                .message("success")
                .build();
        var customerEntity = CustomerEntity.builder()
                .cardId(CARD_UID.getValue())
                .build();
        var operationEntity = OperationEntity.builder()
                .pin("5JR9R1E")
                .fullName("Qurbanov Terlan")
                .customer(customerEntity)
                .build();
        var substrictions = List.of(Subscription.builder()
                .schema("3DS")
                .channel("SMPP_ALL")
                .address("+994513601019")
                .build());
        var subscriptionResponse = SubscriptionResponse.builder().subscriptions(substrictions).build();

        //WHEN
        when(operationRepository.findById(trackId)).thenReturn(Optional.of(operationEntity));
        when(atlasClient.findAllByUID(CARD_UID.getValue(), "", "")).thenReturn(subscriptionResponse);
        lenient().when(otpClient.send(sendOtpRequest)).thenReturn(sendOtpResponse);

        //THEN

        var actual = otpService.send(request);
        var expected = SendOtpResponseDto.builder().maskedMobileNum("*********1019").message("success").build();

        assertEquals(expected, actual);
    }

    @Test
    void verify_Success() {
        var trackId = UUID.randomUUID();
        var customerEntity = CustomerEntity.builder()
                .cardId(CARD_UID.getValue())
                .build();
        var orderEntity = OrderEntity.builder()
                .rrn(RRN.getValue())
                .totalAmount(BigDecimal.valueOf(1500))
                .commission(BigDecimal.valueOf(25))
                .build();
        var orders = List.of(orderEntity);
        var operationEntity = OperationEntity.builder()
                .pin("5JR9R1E")
                .fullName("Qurbanov Terlan")
                .customer(customerEntity)
                .orders(orders)
                .build();
        var substrictions = List.of(Subscription.builder()
                .schema("3DS")
                .channel("SMPP_ALL")
                .address("+994513601019")
                .build());
        var subscriptionResponse = SubscriptionResponse.builder().subscriptions(substrictions).build();
        var verify = OtpVerifyResponse.builder()
                .status("success")
                .build();
        var otpVerifyRequestDto = OtpVerifyRequestDto.builder()
                .otp("2222")
                .trackId(trackId)
                .build();
        var otpVerifyRequest = OtpVerifyRequest.builder()
                .otp(otpVerifyRequestDto.getOtp())
                .phoneNumber("+994513601019")
                .build();
        var purchaseResponse = PurchaseResponse.builder()
                .id("83660ed4-9e42-11ec-b909-0242ac120002")
                .approvalCode("789456")
                .build();

        when(operationRepository.findById(any()))
                .thenReturn(Optional.of(operationEntity));
        when(atlasClient.findAllByUID(CARD_UID.getValue(), "", ""))
                .thenReturn(subscriptionResponse);
        when(otpClient.verify(otpVerifyRequest)).thenReturn(verify);
        when(atlasClient.purchase(any(PurchaseRequest.class))).thenReturn(purchaseResponse);

        var actual = otpService.verify(otpVerifyRequestDto);
        var expected = OtpVerifyResponseDto.builder()
                .trackId(trackId)
                .status("success")
                .build();

        assertEquals(expected, actual);

    }

    @Test
    void verify_When_AtlasClientException() {
        var trackId = UUID.randomUUID();
        var customerEntity = CustomerEntity.builder()
                .cardId(CARD_UID.getValue())
                .build();
        var orderEntity = OrderEntity.builder()
                .rrn(RRN.getValue())
                .totalAmount(BigDecimal.valueOf(1500))
                .commission(BigDecimal.valueOf(25))
                .build();
        var orders = List.of(orderEntity);
        var operationEntity = OperationEntity.builder()
                .pin("5JR9R1E")
                .fullName("Qurbanov Terlan")
                .customer(customerEntity)
                .orders(orders)
                .build();
        var substrictions = List.of(Subscription.builder()
                .schema("3DS")
                .channel("SMPP_ALL")
                .address("+994513601019")
                .build());
        var subscriptionResponse = SubscriptionResponse.builder().subscriptions(substrictions).build();
        var verify = OtpVerifyResponse.builder()
                .status("success")
                .build();
        var otpVerifyRequestDto = OtpVerifyRequestDto.builder()
                .otp("2222")
                .trackId(trackId)
                .build();
        var otpVerifyRequest = OtpVerifyRequest.builder()
                .otp(otpVerifyRequestDto.getOtp())
                .phoneNumber("+994513601019")
                .build();

        when(operationRepository.findById(any()))
                .thenReturn(Optional.of(operationEntity));
        when(atlasClient.findAllByUID(CARD_UID.getValue(), "", ""))
                .thenReturn(subscriptionResponse);
        when(otpClient.verify(otpVerifyRequest)).thenReturn(verify);
        when(atlasClient.purchase(any(PurchaseRequest.class)))
                .thenThrow(new AtlasClientException(UUID.randomUUID(), "000", "testUnit"));

        var actual = otpService.verify(otpVerifyRequestDto);
        var expected = OtpVerifyResponseDto.builder()
                .trackId(trackId)
                .status("FAIL_IN_PURCHASE")
                .build();

        assertEquals(expected, actual);

    }

    @Test
    void verify_When_OtpClientException() {
        var trackId = UUID.randomUUID();
        var customerEntity = CustomerEntity.builder()
                .cardId(CARD_UID.getValue())
                .build();
        var orderEntity = OrderEntity.builder()
                .rrn(RRN.getValue())
                .totalAmount(BigDecimal.valueOf(1500))
                .commission(BigDecimal.valueOf(25))
                .build();
        var orders = List.of(orderEntity);
        var operationEntity = OperationEntity.builder()
                .pin("5JR9R1E")
                .fullName("Qurbanov Terlan")
                .customer(customerEntity)
                .orders(orders)
                .build();
        var substrictions = List.of(Subscription.builder()
                .schema("3DS")
                .channel("SMPP_ALL")
                .address("+994513601019")
                .build());
        var subscriptionResponse = SubscriptionResponse.builder()
                .subscriptions(substrictions)
                .build();
        var otpVerifyRequestDto = OtpVerifyRequestDto.builder()
                .otp("2222")
                .trackId(trackId)
                .build();
        var otpVerifyRequest = OtpVerifyRequest.builder()
                .otp(otpVerifyRequestDto.getOtp())
                .phoneNumber("+994513601019")
                .build();

        when(operationRepository.findById(any()))
                .thenReturn(Optional.of(operationEntity));
        when(atlasClient.findAllByUID(CARD_UID.getValue(), "", ""))
                .thenReturn(subscriptionResponse);
        when(otpClient.verify(otpVerifyRequest)).thenThrow(new OtpClientException("Fail", "test"));

        var actual = otpService.verify(otpVerifyRequestDto);
        var expected = OtpVerifyResponseDto.builder()
                .trackId(trackId)
                .status("message: {} , detail: {} Fail")
                .build();
        ;

        assertEquals(expected, actual);

    }

}
