package az.kapitalbank.marketplace.client.atlas;

import java.util.UUID;

import az.kapitalbank.marketplace.exception.AtlasException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AtlasClientExceptionDecoder implements ErrorDecoder {

    @SneakyThrows
    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 400 || response.status() == 404) {
            var errorResponse =
                    new ObjectMapper().readValue(response.body().asInputStream(), AtlasException.class);
            throw new AtlasException(errorResponse.getUuid(), errorResponse.getCode(), errorResponse.getMessage());
        } else {
            throw new AtlasException(UUID.randomUUID().toString(), "500", "Atlas Client Internal Server Error");
        }
    }
}