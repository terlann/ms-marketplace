package az.kapitalbank.marketplace.client.otp;

import az.kapitalbank.marketplace.client.otp.model.OtpVerifyRequest;
import az.kapitalbank.marketplace.client.otp.model.OtpVerifyResponse;
import az.kapitalbank.marketplace.client.otp.model.SendOtpRequest;
import az.kapitalbank.marketplace.client.otp.model.SendOtpResponse;
import feign.Logger;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "sms-client", url = "${client.ms-otp.url}")
public interface OtpClient {
    @PostMapping("/otp/send")
    SendOtpResponse send(SendOtpRequest sendOtpRequest);

    @PostMapping("/otp/verify")
    OtpVerifyResponse verify(OtpVerifyRequest request);

    class FeignConfiguration {
        @Bean
        Logger.Level loggerLevel() {
            return Logger.Level.FULL;
        }

        @Bean
        OtpClientErrorDecoder errorDecoder() {
            return new OtpClientErrorDecoder();
        }
    }
}
