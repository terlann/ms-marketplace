package az.kapitalbank.marketplace.client.externalinteg;

import az.kapitalbank.marketplace.client.externalinteg.exception.ExternalIntegrationException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ExternalIntegrationErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        return new ExternalIntegrationException(methodKey, response.body().toString());
    }
}
