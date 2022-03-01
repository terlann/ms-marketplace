package az.kapitalbank.marketplace.client.atlas;

import az.kapitalbank.marketplace.client.atlas.exception.AtlasClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AtlasClientErrorDecoder implements ErrorDecoder {

    @SneakyThrows
    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 400 || response.status() == 404) {
            var errorResponse =
                    new ObjectMapper().readValue(response.body().asInputStream(), AtlasClientException.class);
            throw new AtlasClientException(errorResponse.getUuid(),
                    errorResponse.getCode(),
                    errorResponse.getMessage());
        } else {
            throw new AtlasClientException("", String.valueOf(response.status()), response.toString());
        }
    }
}