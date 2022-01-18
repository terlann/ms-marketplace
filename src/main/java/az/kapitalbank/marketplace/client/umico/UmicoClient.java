package az.kapitalbank.marketplace.client.umico;

import az.kapitalbank.marketplace.client.umico.model.UmicoScoringDecisionRequest;
import az.kapitalbank.marketplace.client.umico.model.UmicoScoringDecisionResponse;
import az.kapitalbank.marketplace.client.umico.model.UmicoScoringTrancheRequest;
import feign.Logger;
import feign.codec.ErrorDecoder;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

@FeignClient(name = "client-umico",
        url = "${client.umico.url}",
        primary = false,
        configuration = UmicoClient.FeignConfiguration.class)
public interface UmicoClient {

    @PostMapping("/api/v1/application_offers")
    UmicoScoringDecisionResponse sendDecisionScoring(@RequestBody UmicoScoringDecisionRequest request,
                                                     @RequestHeader("ApiKey") String apiKey);

    @PostMapping("/api/v1/tranche/completed")
    void sendDecisionTranche(@RequestBody UmicoScoringTrancheRequest request,
                             @RequestHeader("ApiKey") String apiKey);


    class FeignConfiguration {
        @Bean
        Logger.Level feignLoggerLevel() {
            return Logger.Level.BASIC;
        }

        @Bean
        public ErrorDecoder feignErrorDecoder() {
            return new UmicoClientErrorDecoder();
        }
    }

}
