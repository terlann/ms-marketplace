package az.kapitalbank.marketplace.service;

import az.kapitalbank.marketplace.client.common.CommonClient;
import az.kapitalbank.marketplace.client.common.model.request.SendSmsRequest;
import az.kapitalbank.marketplace.config.SmsProperties;
import az.kapitalbank.marketplace.entity.OperationEntity;
import feign.FeignException;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SmsService {

    CommonClient commonClient;
    SmsProperties smsProperties;
    OtpService otpService;

    public void sendPreapproveSms(OperationEntity operationEntity) {
        String text = smsProperties.getText().get("preapprove");
        send(operationEntity.getId(), operationEntity.getMobileNumber(), text);
    }

    public void sendCompleteScoringSms(OperationEntity operationEntity) {
        var text = smsProperties.getText().get("complete-scoring")
                .replace("{amount}", operationEntity.getScoredAmount().toString());
        send(operationEntity.getId(), operationEntity.getMobileNumber(), text);
    }

    public void sendPrePurchaseSms(OperationEntity operationEntity) {
        String mobileNumber =
                otpService.getCardLinkedMobileNumber(operationEntity.getCustomer().getCardId());
        String text = smsProperties.getText().get("pre-purchase");
        var purchasedAmount = operationEntity.getTotalAmount().add(operationEntity.getCommission());
        text = text.replace("{amount}", purchasedAmount.toString());
        send(operationEntity.getId(), mobileNumber, text);
    }

    public void sendPendingSms(OperationEntity operationEntity) {
        String text = smsProperties.getText().get("pending");
        send(operationEntity.getId(), operationEntity.getMobileNumber(), text);
    }

    private void send(UUID trackId, String mobileNumber, String text) {
        mobileNumber = mobileNumber.replace("+", "");
        try {
            commonClient.sendSms(new SendSmsRequest(text, mobileNumber));
            log.info("Send sms : text - {}, mobileNumber - {} , trackId - {}", text,
                    mobileNumber, trackId);
        } catch (FeignException ex) {
            log.error("Send sms was failed : text - {} , mobileNumber - {} , exception - {} ", text,
                    mobileNumber, ex);
        }
    }
}
