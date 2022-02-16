package az.kapitalbank.marketplace.client.dvs;

import az.kapitalbank.marketplace.exception.FeignClientException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DvsClientErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        log.error("adp-dvs client error decoder. Response - {}", response.toString());
        return new FeignClientException(methodKey, response.body().toString());
    }
}
