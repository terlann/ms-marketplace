package az.kapitalbank.marketplace.client.checker;

import feign.Logger;
import feign.codec.ErrorDecoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "checker",
        url = "${client.checker.url}",
        primary = false,
        configuration = CheckerClient.FeignConfiguration.class)
public interface CheckerClient {

    @GetMapping("/v1/check/{pinCode}")
    Boolean checkPinCode(@PathVariable String pinCode);

    class FeignConfiguration {

        @Bean
        Logger.Level feignLoggerLevel() {
            return Logger.Level.BASIC;
        }

        @Bean
        public ErrorDecoder feignErrorDecoder() {
            return new CheckerClientErrorDecoder();
        }

    }
}
