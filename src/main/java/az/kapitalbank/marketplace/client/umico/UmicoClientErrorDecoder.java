package az.kapitalbank.marketplace.client.umico;

import az.kapitalbank.marketplace.client.umico.exception.UmicoClientException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UmicoClientErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        return new UmicoClientException(methodKey, response.body().toString());
    }
}
