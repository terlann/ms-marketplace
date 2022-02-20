package az.kapitalbank.marketplace.client.dvs;

import az.kapitalbank.marketplace.client.dvs.exception.DvsClientException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DvsClientErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        return new DvsClientException(methodKey, response.body().toString());
    }
}
