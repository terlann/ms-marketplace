package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constant.OptimusConstant.SALES_SOURCE;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.otp.OtpClient;
import az.kapitalbank.marketplace.client.otp.model.ChannelRequest;
import az.kapitalbank.marketplace.client.otp.model.SendOtpRequest;
import az.kapitalbank.marketplace.client.otp.model.VerifyOtpRequest;
import az.kapitalbank.marketplace.constant.OtpConstant;
import az.kapitalbank.marketplace.dto.request.SendOtpRequestDto;
import az.kapitalbank.marketplace.dto.request.VerifyOtpRequestDto;
import az.kapitalbank.marketplace.dto.response.SendOtpResponseDto;
import az.kapitalbank.marketplace.exception.OperationNotFoundException;
import az.kapitalbank.marketplace.exception.SubscriptionNotFoundException;
import az.kapitalbank.marketplace.repository.OperationRepository;
import az.kapitalbank.marketplace.util.OtpUtil;
import java.util.UUID;
import javax.transaction.Transactional;
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

    OtpClient otpClient;
    AtlasClient atlasClient;
    OperationRepository operationRepository;
    OrderService orderService;

    @Transactional
    public SendOtpResponseDto send(SendOtpRequestDto request) {
        log.info("Send otp process is started : request : {}", request);
        var trackId = request.getTrackId();
        var operationEntity = operationRepository.findById(trackId)
                .orElseThrow(() -> new OperationNotFoundException("trackId: " + trackId));
        var cardId = operationEntity.getCustomer().getCardId();
        String cardLinkedMobileNumber = getCardLinkedMobileNumber(cardId);
        SendOtpRequest sendOtpRequest = SendOtpRequest.builder().phoneNumber(cardLinkedMobileNumber)
                .definitionId(UUID.fromString(OtpConstant.DEFINITION_ID.getValue()))
                .data(new ChannelRequest(SALES_SOURCE)).build();
        log.info("Send otp : request - {}", sendOtpRequest);
        var sendOtpResponse = otpClient.send(sendOtpRequest);
        log.info("Send otp process was finished : response - {}", sendOtpResponse);
        return new SendOtpResponseDto(OtpUtil.maskMobileNumber(cardLinkedMobileNumber));
    }

    @Transactional
    public void verify(VerifyOtpRequestDto request) {
        log.info("Verify otp process is started : request : {}", request);
        var trackId = request.getTrackId();
        var operationEntity = operationRepository.findById(trackId)
                .orElseThrow(() -> new OperationNotFoundException("trackId: " + trackId));
        var customerEntity = operationEntity.getCustomer();
        String cardLinkedMobileNumber = getCardLinkedMobileNumber(customerEntity.getCardId());
        var otpVerifyRequest =
                VerifyOtpRequest.builder().otp(request.getOtp()).phoneNumber(cardLinkedMobileNumber)
                        .build();
        log.info("Verify otp : request - {}", otpVerifyRequest);
        var verifyOtpResponse = otpClient.verify(otpVerifyRequest);
        log.info("Verify otp process was finished : response - {}", verifyOtpResponse);
        var cardId = customerEntity.getCardId();
        orderService.prePurchaseOrders(operationEntity, cardId);
        operationRepository.save(operationEntity);
    }

    private String getCardLinkedMobileNumber(String cardId) {
        log.info("Card linked mobile number process is started : cardId - {}", cardId);
        var subscriptionResponse = atlasClient.findAllByUid(cardId, "", "");
        log.info("AtlasClient subscriptionResponse : {}", subscriptionResponse);
        var cardLinkedMobileNumber = subscriptionResponse.getSubscriptions().stream()
                .filter(subscription -> subscription.getChannel().equals("SMPP_ALL")
                        && subscription.getScheme().contains("3DS"))
                .findFirst()
                .orElseThrow(() -> new SubscriptionNotFoundException("cardId: " + cardId))
                .getAddress();
        log.info(
                "Card linked mobile number process was finished : "
                        + "cardId - {}, cardLinkedMobileNumber - {}",
                cardId, cardLinkedMobileNumber);
        return cardLinkedMobileNumber;
    }
}
