package az.kapitalbank.marketplace.client.integration;

import az.kapitalbank.marketplace.client.integration.exception.IamasClientException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IamasClientErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        return new IamasClientException(methodKey, response.body().toString());
    }
}
