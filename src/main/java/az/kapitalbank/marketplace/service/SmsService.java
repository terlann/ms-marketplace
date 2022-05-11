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

    public void sendSmsPreapprove(OperationEntity operationEntity) {
        String text = smsProperties.getValues().get("preapprove");
        send(operationEntity.getId(), operationEntity.getMobileNumber(), text);
    }

    public void sendSmsCompleteScoring(OperationEntity operationEntity) {
        var text = smsProperties.getValues().get("complete-scoring");
        text = String.format(text, operationEntity.getScoredAmount());
        send(operationEntity.getId(), operationEntity.getMobileNumber(), text);
    }

    public void sendSmsPrePurchase(OperationEntity operationEntity) {
        String mobileNumber =
                otpService.getCardLinkedMobileNumber(operationEntity.getCustomer().getCardId());
        String text = smsProperties.getValues().get("pre-purchase");
        var purchasedAmount = operationEntity.getTotalAmount().add(operationEntity.getCommission());
        text = String.format(text, purchasedAmount);
        send(operationEntity.getId(), mobileNumber, text);
    }

    public void sendSmsPending(OperationEntity operationEntity) {
        String text = smsProperties.getValues().get("pending");
        send(operationEntity.getId(), operationEntity.getMobileNumber(), text);
    }

    private void send(UUID trackId, String mobileNumber, String text) {
        mobileNumber = mobileNumber.replace("+", "");
        try {
            var sendSmsResponse =
                    commonClient.sendSms(new SendSmsRequest(text, mobileNumber));
            log.info("Send sms : Response - {}, mobileNumber - {}", sendSmsResponse,
                    mobileNumber);
        } catch (FeignException ex) {
            log.error("Send sms was failed : trackId - {}, exception - {}", trackId,
                    ex);
        }
    }
}
