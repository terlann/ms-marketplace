package az.kapitalbank.marketplace.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.model.response.Subscription;
import az.kapitalbank.marketplace.client.atlas.model.response.SubscriptionResponse;
import az.kapitalbank.marketplace.client.otp.OtpClient;
import az.kapitalbank.marketplace.client.otp.model.ChannelRequest;
import az.kapitalbank.marketplace.client.otp.model.SendOtpRequest;
import az.kapitalbank.marketplace.client.otp.model.SendOtpResponse;
import az.kapitalbank.marketplace.dto.request.SendOtpRequestDto;
import az.kapitalbank.marketplace.dto.response.SendOtpResponseDto;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.repository.OperationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static az.kapitalbank.marketplace.constants.TestConstants.CARD_UID;
import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @InjectMocks
    OtpService otpService;

    @Test
    void send_Success() {
        //GIVEN
        var request = SendOtpRequestDto.builder()
                .trackId(UUID.fromString("3a30a65a-9bec-11ec-b909-0242ac120002"))
                .build();
        var trackId = UUID.fromString("3a30a65a-9bec-11ec-b909-0242ac120002");

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
        var customerEntity= CustomerEntity.builder()
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
        var subscriptionResponse = SubscriptionResponse.builder()
                .subscriptions(substrictions)
                .build();

        //WHEN
      when(operationRepository.findById(trackId)).thenReturn(Optional.of(operationEntity));
      when(atlasClient.findAllByUID(CARD_UID.getValue(), "", "")).thenReturn(subscriptionResponse);
      lenient().when(otpClient.send(sendOtpRequest)).thenReturn(sendOtpResponse);

        //THEN

        var actual = otpService.send(request);
        var expected = SendOtpResponseDto.builder()
                .maskedMobileNum("*********1019").
                message("success")
                .build();

        assertEquals(expected, actual);
    }

}