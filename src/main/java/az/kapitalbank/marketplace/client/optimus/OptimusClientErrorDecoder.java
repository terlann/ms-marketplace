package az.kapitalbank.marketplace.client.optimus;

import az.kapitalbank.marketplace.client.optimus.exception.OptimusClientException;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OptimusClientErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        return new OptimusClientException(methodKey, response.body().toString());
    }
}
