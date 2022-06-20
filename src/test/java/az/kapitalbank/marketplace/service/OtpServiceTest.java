package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constants.TestConstants.CARD_UID;
import static az.kapitalbank.marketplace.constants.TestConstants.RRN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.model.response.Subscription;
import az.kapitalbank.marketplace.client.atlas.model.response.SubscriptionResponse;
import az.kapitalbank.marketplace.client.otp.OtpClient;
import az.kapitalbank.marketplace.client.otp.model.ChannelRequest;
import az.kapitalbank.marketplace.client.otp.model.SendOtpRequest;
import az.kapitalbank.marketplace.client.otp.model.SendOtpResponse;
import az.kapitalbank.marketplace.client.otp.model.VerifyOtpRequest;
import az.kapitalbank.marketplace.client.otp.model.VerifyOtpResponse;
import az.kapitalbank.marketplace.constant.OtpConstant;
import az.kapitalbank.marketplace.dto.request.SendOtpRequestDto;
import az.kapitalbank.marketplace.dto.request.VerifyOtpRequestDto;
import az.kapitalbank.marketplace.dto.response.SendOtpResponseDto;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.exception.OtpException;
import az.kapitalbank.marketplace.messaging.publisher.PrePurchasePublisher;
import az.kapitalbank.marketplace.repository.OperationRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import feign.Request;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import lombok.SneakyThrows;
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
    OtpClient otpClient;
    @Mock
    AtlasClient atlasClient;
    @Mock
    ObjectMapper objectMapper;
    @Mock
    PrePurchasePublisher prePurchasePublisher;
    @Mock
    OperationRepository operationRepository;
    @InjectMocks
    OtpService otpService;
    @NonFinal
    @Value("${purchase.terminal-name}")
    String terminalName;

    @Test
    void send_Success() {
        var trackId = UUID.fromString(OtpConstant.DEFINITION_ID.getValue());
        var request = SendOtpRequestDto.builder()
                .trackId(trackId).build();
        String cardConnectedNumber = "994553601019";
        SendOtpRequest sendOtpRequest =
                getSendOtpRequest(cardConnectedNumber);
        SendOtpResponse sendOtpResponse = SendOtpResponse.builder()
                .message("success")
                .build();
        OperationEntity operationEntity =
                getOperationEntity();
        SubscriptionResponse subscriptionResponse =
                getSubscriptionResponse();

        when(operationRepository.findById(trackId)).thenReturn(Optional.of(operationEntity));
        when(atlasClient.findAllByUid(CARD_UID.getValue(), "", "")).thenReturn(
                subscriptionResponse);
        when(otpClient.send(any(SendOtpRequest.class))).thenReturn(sendOtpResponse);

        var actual = otpService.send(request);
        var expected =
                SendOtpResponseDto.builder().maskedMobileNumber("99455***1019")
                        .build();

        assertEquals(expected, actual);
    }

    @Test
    @SneakyThrows
    void send_Exception() {
        var trackId = UUID.fromString(OtpConstant.DEFINITION_ID.getValue());
        String cardConnectedNumber = "994553601019";
        OperationEntity operationEntity = getOperationEntity();
        SubscriptionResponse subscriptionResponse = getSubscriptionResponse();
        FeignException feignException = new FeignException.BadRequest("salam",
                Request.create(Request.HttpMethod.GET, "/result", Map.of("salam", List.of("salam")),
                        "{'detail':'salam'}".getBytes(
                                StandardCharsets.UTF_8), null, null),
                "{\"detail\":\"salam\"}".getBytes(
                        StandardCharsets.UTF_8));
        when(operationRepository.findById(trackId)).thenReturn(Optional.of(operationEntity));
        when(atlasClient.findAllByUid(CARD_UID.getValue(), "", "")).thenReturn(
                subscriptionResponse);
        when(otpClient.send(any(SendOtpRequest.class))).thenThrow(feignException);

        var request = SendOtpRequestDto.builder()
                .trackId(trackId).build();
        assertThrows(OtpException.class, () -> otpService.send(request));
    }

    private SubscriptionResponse getSubscriptionResponse() {
        List<Subscription> subscriptions = List.of(Subscription.builder()
                .scheme("3DS")
                .channel("SMPP_ALL")
                .address("994553601019")
                .build());
        return SubscriptionResponse.builder().subscriptions(subscriptions).build();
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
                .definitionId(UUID.fromString(OtpConstant.DEFINITION_ID.getValue()))
                .data(ChannelRequest.builder()
                        .channel("Umico Marketplace")
                        .build())
                .build();
    }

    @Test
    void verify_Success() {
        var trackId = UUID.randomUUID();
        OperationEntity operationEntity = getOperationEntity();
        var subscriptionResponse = getSubscriptionResponse();

        var otpVerifyResponse = VerifyOtpResponse.builder().build();
        when(operationRepository.findById(any()))
                .thenReturn(Optional.of(operationEntity));
        when(atlasClient.findAllByUid(CARD_UID.getValue(), "", ""))
                .thenReturn(subscriptionResponse);
        when(otpClient.verify(any(VerifyOtpRequest.class))).thenReturn(otpVerifyResponse);
        var requestDto = VerifyOtpRequestDto.builder()
                .otp("2222")
                .trackId(trackId)
                .build();

        otpService.verify(requestDto);

        verify(otpClient).verify(any(VerifyOtpRequest.class));
    }

    @Test
    void verify_Exception() {
        var trackId = UUID.randomUUID();
        OperationEntity operationEntity = getOperationEntity();
        var subscriptionResponse = getSubscriptionResponse();
        FeignException feignException = new FeignException.BadRequest("salam",
                Request.create(Request.HttpMethod.GET, "/result", Map.of("salam", List.of("salam")),
                        "{'detail':'salam'}".getBytes(
                                StandardCharsets.UTF_8), null, null),
                "{\"detail\":\"salam\"}".getBytes(
                        StandardCharsets.UTF_8));
        var otpVerifyResponse = VerifyOtpResponse.builder().build();
        when(operationRepository.findById(any()))
                .thenReturn(Optional.of(operationEntity));
        when(atlasClient.findAllByUid(CARD_UID.getValue(), "", ""))
                .thenReturn(subscriptionResponse);
        when(otpClient.verify(any(VerifyOtpRequest.class))).thenThrow(feignException);
        var requestDto = VerifyOtpRequestDto.builder()
                .otp("2222")
                .trackId(trackId)
                .build();

        assertThrows(OtpException.class, () -> otpService.verify(requestDto));

        verify(otpClient).verify(any(VerifyOtpRequest.class));
    }
}
