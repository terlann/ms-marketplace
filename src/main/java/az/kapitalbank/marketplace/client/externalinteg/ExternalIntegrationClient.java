package az.kapitalbank.marketplace.client.externalinteg;

import java.util.List;

import az.kapitalbank.marketplace.client.externalinteg.model.IamasResponse;
import feign.Logger;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "iamas",
        url = "${client.externalinteg.url}",
        primary = false,
        configuration = ExternalIntegrationClient.FeignConfiguration.class)
public interface ExternalIntegrationClient {

    @GetMapping("/iamas/id-card/by/pin/{pin}")
    List<IamasResponse> getData(@PathVariable("pin") String pin);

    class FeignConfiguration {
        @Bean
        Logger.Level loggerLevel() {
            return Logger.Level.FULL;
        }

        @Bean
        ExternalIntegrationErrorDecoder errorDecoder() {
            return new ExternalIntegrationErrorDecoder();
        }
    }
}
