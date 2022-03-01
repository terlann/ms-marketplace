package az.kapitalbank.marketplace.client.telesales;

import az.kapitalbank.marketplace.client.telesales.exception.TelesalesClientException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TelesalesClientErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        return new TelesalesClientException(methodKey, response.body().toString());
    }
}
