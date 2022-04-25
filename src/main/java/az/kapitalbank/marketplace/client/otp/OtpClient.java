package az.kapitalbank.marketplace.client.otp;

import az.kapitalbank.marketplace.client.otp.model.SendOtpRequest;
import az.kapitalbank.marketplace.client.otp.model.SendOtpResponse;
import az.kapitalbank.marketplace.client.otp.model.VerifyOtpRequest;
import az.kapitalbank.marketplace.client.otp.model.VerifyOtpResponse;
import feign.Logger;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "otp-client",
        url = "${client.ms-otp.url}",
        configuration = OtpClient.FeignConfiguration.class)
public interface OtpClient {
    @PostMapping("/otp/send")
    SendOtpResponse send(SendOtpRequest request);

    @PostMapping("/otp/verify")
    VerifyOtpResponse verify(VerifyOtpRequest request);

    class FeignConfiguration {
        @Bean
        Logger.Level loggerLevel() {
            return Logger.Level.FULL;
        }
    }
}
