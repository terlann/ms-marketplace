package az.kapitalbank.marketplace.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.model.request.PurchaseRequest;
import az.kapitalbank.marketplace.client.atlas.model.response.PurchaseResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.Subscription;
import az.kapitalbank.marketplace.client.atlas.model.response.SubscriptionResponse;
import az.kapitalbank.marketplace.client.otp.OtpClient;
import az.kapitalbank.marketplace.client.otp.model.ChannelRequest;
import az.kapitalbank.marketplace.client.otp.model.OtpVerifyRequest;
import az.kapitalbank.marketplace.client.otp.model.OtpVerifyResponse;
import az.kapitalbank.marketplace.client.otp.model.SendOtpRequest;
import az.kapitalbank.marketplace.client.otp.model.SendOtpResponse;
import az.kapitalbank.marketplace.constant.Currency;
import az.kapitalbank.marketplace.constant.TransactionStatus;
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
import static az.kapitalbank.marketplace.constants.TestConstants.TRACK_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
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
        var trackId = UUID.fromString(TRACK_ID.getValue());
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
        var otpVerifyRequest = OtpVerifyRequest.builder()
                .otp("2222")
                .phoneNumber("+994513601019")
                .build();
        var verify = OtpVerifyResponse.builder()
                .status("success")
                .build();
        var otpVerifyRequestDto = OtpVerifyRequestDto.builder()
                .otp("2222")
                .trackId(trackId)
                .build();
        var purchaseRequest = PurchaseRequest.builder()
                .rrn(anyString())
                .amount(orderEntity.getTotalAmount().add(orderEntity.getCommission()))
                .description("fee=" + orderEntity.getCommission())
                .currency(Currency.AZN.getCode())
                .terminalName(terminalName)
                .uid(CARD_UID.getValue())
                .build();
        var purchaseResponse = PurchaseResponse.builder()
                .id("83660ed4-9e42-11ec-b909-0242ac120002")
                .approvalCode("789456")
                .build();


        lenient().when(operationRepository.findById(UUID.fromString(TRACK_ID.getValue())))
                .thenReturn(Optional.of(operationEntity));
        lenient().when(atlasClient.findAllByUID(CARD_UID.getValue(), "", ""))
                .thenReturn(subscriptionResponse);
        lenient().when(otpClient.verify(otpVerifyRequest)).thenReturn(verify);
        lenient().when(atlasClient.purchase(purchaseRequest)).thenReturn(purchaseResponse);

    }
}
