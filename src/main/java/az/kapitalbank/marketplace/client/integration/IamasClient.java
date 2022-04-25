package az.kapitalbank.marketplace.client.integration;

import az.kapitalbank.marketplace.client.integration.model.IamasResponse;
import feign.Logger;
import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "iamas",
        url = "${client.externalinteg.url}",
        primary = false,
        configuration = IamasClient.FeignConfiguration.class)
public interface IamasClient {

    @GetMapping("/iamas/id-card/by/pin/{pin}")
    List<IamasResponse> findPersonByPin(@PathVariable("pin") String pin);

    class FeignConfiguration {
        @Bean
        Logger.Level loggerLevel() {
            return Logger.Level.FULL;
        }
    }
}