package az.kapitalbank.marketplace.client.atlas;

import az.kapitalbank.marketplace.client.atlas.model.request.CompletePrePurchaseRequest;
import az.kapitalbank.marketplace.client.atlas.model.request.PrePurchaseRequest;
import az.kapitalbank.marketplace.client.atlas.model.request.RefundRequest;
import az.kapitalbank.marketplace.client.atlas.model.response.CardDetailResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.CompletePrePurchaseResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.PrePurchaseResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.RefundResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.SubscriptionResponse;
import az.kapitalbank.marketplace.constant.ResultType;
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
        url = "${client.adp-atlas.url}/api/v1",
        configuration = AtlasClient.FeignConfiguration.class)
public interface AtlasClient {

    @PostMapping("/transfers/purchase")
    PrePurchaseResponse prePurchase(@RequestBody PrePurchaseRequest request);

    @PutMapping("/transfers/complete")
    CompletePrePurchaseResponse completePrePurchase(
            @RequestBody CompletePrePurchaseRequest request);

    @PutMapping("/transfers/{id}/refund")
    RefundResponse refund(@PathVariable String id, @RequestBody RefundRequest request);

    @GetMapping("/cards/uid/{uid}")
    CardDetailResponse findCardByUid(@PathVariable String uid,
                                     @RequestParam ResultType resultType);

    @GetMapping("/card-messaging/cards/{uid}/subscriptions")
    SubscriptionResponse findAllByUid(@PathVariable String uid,
                                      @RequestParam String channel,
                                      @RequestParam String schema);

    class FeignConfiguration {
        @Bean
        Logger.Level feignLoggerLevel() {
            return Logger.Level.FULL;
        }

        @Bean
        AtlasClientErrorDecoder exceptionDecoder() {
            return new AtlasClientErrorDecoder();
        }
    }

}
