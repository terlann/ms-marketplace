package az.kapitalbank.marketplace.client.optimus;

import az.kapitalbank.marketplace.exception.models.FeignClientException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class OptimusClientErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("adp-optimus client error decoder. Response - {}", response.toString());
        throw new FeignClientException(methodKey, response.body().toString());
    }
}
