package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constant.OptimusConstant.OTP_SOURCE;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.otp.OtpClient;
import az.kapitalbank.marketplace.client.otp.model.ChannelRequest;
import az.kapitalbank.marketplace.client.otp.model.OtpClientErrorResponse;
import az.kapitalbank.marketplace.client.otp.model.SendOtpRequest;
import az.kapitalbank.marketplace.client.otp.model.VerifyOtpRequest;
import az.kapitalbank.marketplace.constant.Error;
import az.kapitalbank.marketplace.constant.OtpConstant;
import az.kapitalbank.marketplace.dto.request.SendOtpRequestDto;
import az.kapitalbank.marketplace.dto.request.VerifyOtpRequestDto;
import az.kapitalbank.marketplace.dto.response.SendOtpResponseDto;
import az.kapitalbank.marketplace.exception.CommonException;
import az.kapitalbank.marketplace.exception.OtpException;
import az.kapitalbank.marketplace.messaging.event.PrePurchaseEvent;
import az.kapitalbank.marketplace.messaging.publisher.PrePurchasePublisher;
import az.kapitalbank.marketplace.repository.OperationRepository;
import az.kapitalbank.marketplace.util.OtpUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.FeignException;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpService {

    OtpClient otpClient;
    AtlasClient atlasClient;
    ObjectMapper objectMapper;
    PrePurchasePublisher prePurchasePublisher;
    OperationRepository operationRepository;

    @Transactional
    public SendOtpResponseDto send(SendOtpRequestDto request) {
        log.info("Send otp process is started : request - {}", request);
        var trackId = request.getTrackId();
        var operationEntity = operationRepository.findById(trackId)
                .orElseThrow(() -> new CommonException(Error.OPERATION_NOT_FOUND,
                        "Operation not found : trackId" + trackId));
        var cardId = operationEntity.getCustomer().getCardId();
        String cardLinkedMobileNumber = getCardLinkedMobileNumber(cardId);
        SendOtpRequest sendOtpRequest = SendOtpRequest.builder().phoneNumber(cardLinkedMobileNumber)
                .definitionId(UUID.fromString(OtpConstant.DEFINITION_ID.getValue()))
                .data(new ChannelRequest(OTP_SOURCE)).build();
        log.info("Send otp client process is started : trackId - {}, request - {}", trackId,
                sendOtpRequest);
        try {
            var sendOtpResponse = otpClient.send(sendOtpRequest);
            log.info("Send otp client process was finished : trackId - {}, response - {}", trackId,
                    sendOtpResponse);
        } catch (FeignException e) {
            log.error("Send otp client process was failed : trackId - {}, exception - {}",
                    trackId, e);
            throw new OtpException(getOtpClientErrorResponse(e.contentUTF8()).getDetail());
        }
        log.info("Send otp process was finished : trackId - {}", trackId);
        return new SendOtpResponseDto(OtpUtil.maskMobileNumber(cardLinkedMobileNumber));
    }

    @Transactional
    public void verify(VerifyOtpRequestDto request) {
        log.info("Verify otp process is started : request - {}", request);
        var trackId = request.getTrackId();
        var operationEntity = operationRepository.findById(trackId)
                .orElseThrow(() -> new CommonException(Error.OPERATION_NOT_FOUND,
                        "Operation not found.TrackId - " + trackId));
        var cardId = operationEntity.getCustomer().getCardId();
        String cardLinkedMobileNumber = getCardLinkedMobileNumber(cardId);
        var otpVerifyRequest =
                VerifyOtpRequest.builder().otp(request.getOtp()).phoneNumber(cardLinkedMobileNumber)
                        .build();
        log.info("Verify otp client process is started : trackId - {}, request - {}", trackId,
                otpVerifyRequest);
        try {
            var verifyOtpResponse = otpClient.verify(otpVerifyRequest);
            log.info("Verify otp client process was finished : trackId - {}, response - {}",
                    trackId, verifyOtpResponse);
        } catch (FeignException e) {
            log.error("Verify otp client process was failed : trackId - {}, exception - {}",
                    trackId, e);
            throw new OtpException(getOtpClientErrorResponse(e.contentUTF8()).getDetail());
        }
        prePurchasePublisher.sendEvent(new PrePurchaseEvent(trackId));
        log.info("Verify otp process was finished : trackId - {}", trackId);
    }

    @SneakyThrows
    private OtpClientErrorResponse getOtpClientErrorResponse(String otpClientResponse) {
        return objectMapper.readValue(otpClientResponse, OtpClientErrorResponse.class);
    }

    public String getCardLinkedMobileNumber(String cardId) {
        log.info("Card linked mobile number process is started : cardId - {}", cardId);
        var subscriptionResponse = atlasClient.findAllByUid(cardId, "", "");
        log.info("AtlasClient subscriptionResponse : {}", subscriptionResponse);
        var cardLinkedMobileNumber = subscriptionResponse.getSubscriptions().stream()
                .filter(subscription -> subscription.getChannel().equals("SMPP_ALL")
                        && subscription.getScheme().contains("3DS"))
                .findFirst()
                .orElseThrow(() -> new CommonException(Error.SUBSCRIPTION_NOT_FOUND,
                        "Card UID related mobile number not found : cardId: " + cardId))
                .getAddress();
        log.info(
                "Card linked mobile number process was finished : "
                        + "cardId - {}, cardLinkedMobileNumber - {}",
                cardId, cardLinkedMobileNumber);
        return cardLinkedMobileNumber;
    }
}
