package az.kapitalbank.marketplace.client.umico;

import az.kapitalbank.marketplace.client.umico.model.UmicoDecisionRequest;
import az.kapitalbank.marketplace.client.umico.model.UmicoDecisionResponse;
import feign.Logger;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "client-umico",
        url = "${client.umico.url}/api/v2",
        primary = false,
        configuration = UmicoClient.FeignConfiguration.class)
public interface UmicoClient {

    @PostMapping("/application_offers")
    UmicoDecisionResponse sendDecisionToUmico(@RequestBody UmicoDecisionRequest request,
                                              @RequestHeader("ApiKey") String apiKey);

    class FeignConfiguration {
        @Bean
        Logger.Level loggerLevel() {
            return Logger.Level.FULL;
        }

        @Bean
        UmicoClientErrorDecoder errorDecoder() {
            return new UmicoClientErrorDecoder();
        }
    }
}
