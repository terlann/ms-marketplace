package az.kapitalbank.marketplace.client.atlas;

import az.kapitalbank.marketplace.exception.AtlasException;
import az.kapitalbank.marketplace.exception.FeignClientException;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AtlasClientExceptionDecoder implements ErrorDecoder {

    @SneakyThrows
    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 400) {
            throw new AtlasException(methodKey,response.toString());
// TODO learn atlas available exceptions and throw
        } else
            throw new FeignClientException(methodKey, response.toString());
    }
}
