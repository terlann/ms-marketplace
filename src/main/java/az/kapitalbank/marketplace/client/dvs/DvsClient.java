package az.kapitalbank.marketplace.client.dvs;

import java.util.UUID;

import az.kapitalbank.marketplace.client.dvs.model.DvsGetDetailsResponse;
import feign.Logger;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;


@FeignClient(name = "dvs-client",
        url = "${client.adp-dvs.url}",
        configuration = DvsClient.FeignConfiguration.class)
public interface DvsClient {

    @GetMapping("/dvs/link/{order-id}/{dvs-id}")
    DvsGetDetailsResponse getDetails(@PathVariable("order-id") UUID trackId, @PathVariable("dvs-id") Long dvsId);

    class FeignConfiguration {
        @Bean
        Logger.Level loggerLevel() {
            return Logger.Level.FULL;
        }

        @Bean
        DvsClientErrorDecoder errorDecoder() {
            return new DvsClientErrorDecoder();
        }

    }

}
