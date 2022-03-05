package az.kapitalbank.marketplace.client.otp;

import az.kapitalbank.marketplace.client.otp.model.SendOtpRequest;
import az.kapitalbank.marketplace.client.otp.model.SendOtpResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "sms-client", url = "${ms-otp.url}")
public interface OtpClient {
    @PostMapping("/otp/send")
    SendOtpResponse send(SendOtpRequest sendOtpRequest);
}
