package az.kapitalbank.marketplace.client.umico;

import az.kapitalbank.marketplace.exception.models.FeignClientException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class UmicoClientErrorDecoder implements ErrorDecoder {
    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("umico client error decoder. Response - {}", response.toString());
        throw new FeignClientException(methodKey, response.body().toString());
    }
}
