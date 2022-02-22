package az.kapitalbank.marketplace.client.atlas;

import az.kapitalbank.marketplace.client.atlas.model.request.PurchaseCompleteRequest;
import az.kapitalbank.marketplace.client.atlas.model.request.PurchaseRequest;
import az.kapitalbank.marketplace.client.atlas.model.request.ReversPurchaseRequest;
import az.kapitalbank.marketplace.client.atlas.model.response.CardDetailResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.CardResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.PurchaseCompleteResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.PurchaseResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.ReverseResponse;
import az.kapitalbank.marketplace.constants.ResultType;
import feign.Logger;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "adp-atlas",
        url = "${client.adp-atlas.url}",
        configuration = AtlasClient.FeignConfiguration.class)
public interface AtlasClient {

    @PostMapping("/api/v1/transfers/purchase")
    PurchaseResponse purchase(@RequestBody PurchaseRequest request);

    @PutMapping("/api/v1/transfers/complete")
    PurchaseCompleteResponse complete(@RequestBody PurchaseCompleteRequest request);

    @PutMapping("/api/v1/transfers/{id}/reverse")
    ReverseResponse reverse(@PathVariable String id, @RequestBody ReversPurchaseRequest request);

    @GetMapping("/uid/{uid}")
    CardDetailResponse findCardByUID(@PathVariable String uid,
                                     @RequestParam(required = false) ResultType resultType);

    @GetMapping("/api/v1/cards/pan/{pan}")
    CardResponse findByPan(@PathVariable String pan);


    class FeignConfiguration {
        @Bean
        Logger.Level feignLoggerLevel() {
            return Logger.Level.BASIC;
        }

        @Bean
        AtlasClientErrorDecoder exceptionDecoder() {
            return new AtlasClientErrorDecoder();
        }
    }

}
