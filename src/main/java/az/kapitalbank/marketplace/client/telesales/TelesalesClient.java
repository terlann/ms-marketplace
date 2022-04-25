package az.kapitalbank.marketplace.client.telesales;

import az.kapitalbank.marketplace.client.telesales.model.CreateTelesalesOrderRequest;
import az.kapitalbank.marketplace.client.telesales.model.CreateTelesalesOrderResponse;
import feign.Logger;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "telesales-ete",
        url = "${client.ete.url}",
        configuration = TelesalesClient.FeignConfiguration.class)
public interface TelesalesClient {

    @PostMapping("/SRS/CreateCustomerOperation")
    CreateTelesalesOrderResponse sendLead(@RequestBody CreateTelesalesOrderRequest request);

    class FeignConfiguration {
        @Bean
        Logger.Level loggerLevel() {
            return Logger.Level.FULL;
        }
    }
}
