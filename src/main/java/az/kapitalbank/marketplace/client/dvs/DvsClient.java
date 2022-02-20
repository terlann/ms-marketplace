package az.kapitalbank.marketplace.client.dvs;

import az.kapitalbank.marketplace.client.dvs.model.DvsCreateOrderRequest;
import az.kapitalbank.marketplace.client.dvs.model.DvsCreateOrderResponse;
import az.kapitalbank.marketplace.client.dvs.model.DvsGetDetailsResponse;
import feign.Logger;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@FeignClient(name = "dvs-client",
        url = "${client.adp-dvs.url}",
        configuration = DvsClient.FeignConfiguration.class)
public interface DvsClient {

    @GetMapping("/dvs/link/{order-id}/{dvs-id}")
    DvsGetDetailsResponse getDetails(@PathVariable("order-id") String orderId, @PathVariable("dvs-id") String dvsId);

    @PostMapping("/dvs")
    DvsCreateOrderResponse createOrder(@RequestBody DvsCreateOrderRequest request);

    class FeignConfiguration {
        @Bean
        Logger.Level loggerLevel() {
            return Logger.Level.BASIC;
        }

        @Bean
        DvsClientErrorDecoder errorDecoder() {
            return new DvsClientErrorDecoder();
        }

    }

}
