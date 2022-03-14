package az.kapitalbank.marketplace.service;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.model.request.PurchaseRequest;
import az.kapitalbank.marketplace.client.otp.OtpClient;
import az.kapitalbank.marketplace.client.otp.model.ChannelRequest;
import az.kapitalbank.marketplace.client.otp.model.OtpVerifyRequest;
import az.kapitalbank.marketplace.client.otp.model.SendOtpRequest;
import az.kapitalbank.marketplace.constant.Currency;
import az.kapitalbank.marketplace.constant.OtpConstant;
import az.kapitalbank.marketplace.constant.TransactionStatus;
import az.kapitalbank.marketplace.dto.request.OtpVerifyRequestDto;
import az.kapitalbank.marketplace.dto.request.SendOtpRequestDto;
import az.kapitalbank.marketplace.dto.response.SendOtpResponseDto;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.exception.OperationNotFoundException;
import az.kapitalbank.marketplace.exception.SubscriptionNotFoundException;
import az.kapitalbank.marketplace.repository.OperationRepository;
import az.kapitalbank.marketplace.repository.OrderRepository;
import az.kapitalbank.marketplace.util.GenerateUtil;
import az.kapitalbank.marketplace.util.OtpUtil;
import java.util.ArrayList;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OtpService {
    AtlasClient atlasClient;
    OtpClient otpClient;
    OperationRepository operationRepository;
    OrderRepository orderRepository;
    @NonFinal
    @Value("${purchase.terminal-name}")
    String terminalName;

    @Transactional
    public SendOtpResponseDto send(SendOtpRequestDto request) {
        log.info("Starting send otp service. Request : {}", request);
        String cardConnectedNumber = getMobileNumber(request.getTrackId());
        log.info("Sending OTP: Mobile Number: " + cardConnectedNumber);
        SendOtpRequest sendOtpRequest = SendOtpRequest.builder()
                .phoneNumber(cardConnectedNumber)
                .definitionId(UUID.fromString(OtpConstant.DEFINITION_ID.getValue()))
                .channel(new ChannelRequest("Umico Marketplace"))
                .build();
        var sendOtp = otpClient.send(sendOtpRequest);
        log.info("Sended OTP Response: " + sendOtp.getMessage());

        return new SendOtpResponseDto(OtpUtil.maskMobileNumber(cardConnectedNumber));
    }

    @Transactional
    public void verify(OtpVerifyRequestDto request) {
        var operationEntity = operationRepository.findById(request.getTrackId())
                .orElseThrow(
                        () -> new OperationNotFoundException("trackId: " + request.getTrackId()));
        var customerEntity = operationEntity.getCustomer();
        String cardConnectedNumber = getMobileNumber(request.getTrackId());
        log.info("Verifing OTP: Mobile Number - {} , TrackId - {}: " + cardConnectedNumber,
                request.getTrackId());
        var otpVerifyRequest = OtpVerifyRequest.builder()
                .otp(request.getOtp())
                .phoneNumber(cardConnectedNumber)
                .build();
        var verify = otpClient.verify(otpVerifyRequest);
        log.info("Verifed OTP Response: - " + verify.getStatus());
        prePurchaseOrder(operationEntity, customerEntity);
    }

    private void prePurchaseOrder(OperationEntity operationEntity, CustomerEntity customerEntity) {
        log.info("Purchase process began. TrackId: " + operationEntity.getId());

        var orderEntities = operationEntity.getOrders();
        var purchasedOrders = new ArrayList<OrderEntity>();
        var cardId = customerEntity.getCardId();
        for (var orderEntity : orderEntities) {
            var rrn = GenerateUtil.rrn();
            var purchaseRequest = PurchaseRequest.builder()
                    .rrn(rrn)
                    .amount(orderEntity.getTotalAmount().add(orderEntity.getCommission()))
                    .description("fee=" + orderEntity.getCommission())
                    .currency(Currency.AZN.getCode())
                    .terminalName(terminalName)
                    .uid(cardId)
                    .build();
            var purchaseResponse = atlasClient.purchase(purchaseRequest);
            orderEntity.setRrn(rrn);
            orderEntity.setTransactionId(purchaseResponse.getId());
            orderEntity.setApprovalCode(purchaseResponse.getApprovalCode());
            orderEntity.setTransactionStatus(TransactionStatus.PURCHASE);
            purchasedOrders.add(orderEntity);
        }
        orderRepository.saveAll(purchasedOrders);
        log.info("Regular order was created with purchase. customerId - {}, trackId - {}",
                customerEntity.getId(), operationEntity.getId());
    }

    private String getMobileNumber(UUID trackId) {
        log.info("get mobile number: " + trackId);
        var operationEntity = operationRepository.findById(trackId)
                .orElseThrow(() -> new OperationNotFoundException("trackId: " + trackId));
        var cardId = operationEntity.getCustomer().getCardId();
        log.info("Card UUID: " + cardId);
        var subscriptionResponse = atlasClient
                .findAllByUid(cardId, "", "");
        return subscriptionResponse.getSubscriptions().stream()
                .filter(subscription -> subscription.getChannel().equals("SMPP_ALL")
                        && subscription.getScheme().contains("3DS")).findFirst()
                .orElseThrow(() -> new SubscriptionNotFoundException("CardUID: " + cardId))
                .getAddress();
    }
}
