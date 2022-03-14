package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constants.TestConstants.CARD_UID;
import static az.kapitalbank.marketplace.constants.TestConstants.RRN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
import az.kapitalbank.marketplace.dto.request.OtpVerifyRequestDto;
import az.kapitalbank.marketplace.dto.request.SendOtpRequestDto;
import az.kapitalbank.marketplace.dto.response.SendOtpResponseDto;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.repository.OperationRepository;
import az.kapitalbank.marketplace.repository.OrderRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.experimental.NonFinal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;

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
        String cardConnectedNumber = "994553601019";
        SendOtpRequest sendOtpRequest =
                getSendOtpRequest(cardConnectedNumber);
        SendOtpResponse sendOtpResponse = SendOtpResponse.builder()
                .message("success")
                .build();
        var customerEntity = CustomerEntity.builder()
                .cardId(CARD_UID.getValue())
                .build();
        OperationEntity operationEntity =
                getOperationEntity(customerEntity);
        SubscriptionResponse subscriptionResponse =
                getSubscriptionResponse();

        when(operationRepository.findById(trackId)).thenReturn(Optional.of(operationEntity));
        when(atlasClient.findAllByUid(CARD_UID.getValue(), "", "")).thenReturn(
                subscriptionResponse);
        when(otpClient.send(sendOtpRequest)).thenReturn(sendOtpResponse);

        var actual = otpService.send(request);
        var expected =
                SendOtpResponseDto.builder().maskedMobileNumber("99455***1019")
                        .build();

        assertEquals(expected, actual);
    }

    private SubscriptionResponse getSubscriptionResponse() {
        List<Subscription> subscriptions = List.of(Subscription.builder()
                .scheme("3DS")
                .channel("SMPP_ALL")
                .address("994553601019")
                .build());
        return SubscriptionResponse.builder().subscriptions(subscriptions).build();
    }


    private OperationEntity getOperationEntity(CustomerEntity customerEntity) {
        return OperationEntity.builder()
                .pin("5JR9R1E")
                .fullName("Qurbanov Terlan")
                .customer(customerEntity)
                .build();
    }

    private OperationEntity getOperationEntity(CustomerEntity customerEntity,
                                               List<OrderEntity> orders) {
        return OperationEntity.builder()
                .pin("5JR9R1E")
                .fullName("Qurbanov Terlan")
                .customer(customerEntity)
                .orders(orders)
                .build();
    }

    private OperationEntity getOperationEntity() {
        var customerEntity = CustomerEntity.builder()
                .cardId(CARD_UID.getValue())
                .build();
        var orderEntity = OrderEntity.builder()
                .rrn(RRN.getValue())
                .totalAmount(BigDecimal.valueOf(1500))
                .commission(BigDecimal.valueOf(25))
                .build();
        var orders = List.of(orderEntity);
        return getOperationEntity(customerEntity, orders);
    }

    private SendOtpRequest getSendOtpRequest(String cardConnectedNumber) {
        return SendOtpRequest.builder()
                .phoneNumber(cardConnectedNumber)
                .definitionId(UUID.fromString("00608fa-9bae-11ec-b909-0242ac120002"))
                .channel(ChannelRequest.builder()
                        .channel("Umico Marketplace")
                        .build())
                .build();
    }

    @Test
    void verify_Success() {
        var trackId = UUID.randomUUID();
        OperationEntity operationEntity = getOperationEntity();
        var subscriptionResponse = getSubscriptionResponse();

        var purchaseResponse = PurchaseResponse.builder()
                .id("83660ed4-9e42-11ec-b909-0242ac120002")
                .approvalCode("789456")
                .build();
        var otpVerifyResponse = OtpVerifyResponse.builder().build();
        when(operationRepository.findById(any()))
                .thenReturn(Optional.of(operationEntity));
        when(atlasClient.findAllByUid(CARD_UID.getValue(), "", ""))
                .thenReturn(subscriptionResponse);
        when(atlasClient.purchase(any(PurchaseRequest.class))).thenReturn(purchaseResponse);
        when(otpClient.verify(any(OtpVerifyRequest.class))).thenReturn(otpVerifyResponse);

        var requestDto = OtpVerifyRequestDto.builder()
                .otp("2222")
                .trackId(trackId)
                .build();
        otpService.verify(requestDto);

        verify(atlasClient).purchase(any(PurchaseRequest.class));

    }
}
