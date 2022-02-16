package az.kapitalbank.marketplace.client.atlas;

import az.kapitalbank.marketplace.exception.AtlasException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AtlasClientExceptionDecoder implements ErrorDecoder {

    @SneakyThrows
    @Override
    public Exception decode(String methodKey, Response response) {

        throw new AtlasException(methodKey, response.toString());

    }
}
