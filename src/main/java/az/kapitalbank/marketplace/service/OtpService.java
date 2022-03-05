package az.kapitalbank.marketplace.service;

import java.util.UUID;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.otp.OtpClient;
import az.kapitalbank.marketplace.client.otp.model.ChannelRequest;
import az.kapitalbank.marketplace.client.otp.model.SendOtpRequest;
import az.kapitalbank.marketplace.dto.request.SendOtpRequestDto;
import az.kapitalbank.marketplace.dto.response.SendOtpResponseDto;
import az.kapitalbank.marketplace.exception.OperationNotFoundException;
import az.kapitalbank.marketplace.exception.SubscriptionNotFoundException;
import az.kapitalbank.marketplace.repository.OperationRepository;
import az.kapitalbank.marketplace.util.MaskedMobileNum;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpService {
    AtlasClient atlasClient;
    OtpClient otpClient;
    OperationRepository operationRepository;

    public SendOtpResponseDto send(SendOtpRequestDto request) {
        String cardConnectedNumber = getMobileNumber(request.getTrackId());
        SendOtpRequest sendOtpRequest = SendOtpRequest.builder()
                .phoneNumber(cardConnectedNumber)
                .definitionId(UUID.fromString("00608fa-9bae-11ec-b909-0242ac120002"))
                .channel(ChannelRequest.builder()
                        .channel("Umico Marketplace")
                        .build())
                .build();
        var sendOtp = otpClient.send(sendOtpRequest);

        return new SendOtpResponseDto(sendOtp.getMessage(), MaskedMobileNum.maskedMobNumber(cardConnectedNumber));
    }

    private String getMobileNumber(UUID trackId) {
        var operationEntity = operationRepository.findById(trackId)
                .orElseThrow(() -> new OperationNotFoundException("trackId: " + trackId));
        var cardUid = operationEntity.getCustomer().getCardId();
        var subscriptionResponse = atlasClient.findAllByUID(cardUid, "", "");
        return subscriptionResponse.getSubscriptions().stream()
                .filter(subscription -> subscription.getChannel().equals("SMPP_ALL") &&
                        subscription.getSchema().contains("3DS")).findFirst()
                .orElseThrow(() -> new SubscriptionNotFoundException("CardUID: " + cardUid)).getAddress();
    }
}
