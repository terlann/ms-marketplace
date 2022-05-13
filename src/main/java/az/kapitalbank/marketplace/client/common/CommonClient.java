package az.kapitalbank.marketplace.client.common;

import az.kapitalbank.marketplace.client.common.model.request.SendSmsRequest;
import az.kapitalbank.marketplace.client.common.model.response.SendSmsResponse;
import feign.Logger;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "adp-common",
        url = "${client.adp-common.url}",
        configuration = CommonClient.FeignConfiguration.class
)
public interface CommonClient {
    @PostMapping("/sms/send")
    SendSmsResponse sendSms(@RequestBody SendSmsRequest request);

    class FeignConfiguration {
        @Bean
        feign.Logger.Level feignLoggerLevel() {
            return Logger.Level.FULL;
        }
    }
}
