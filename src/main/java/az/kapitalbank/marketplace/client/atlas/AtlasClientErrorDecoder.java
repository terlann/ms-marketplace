package az.kapitalbank.marketplace.client.atlas;

import az.kapitalbank.marketplace.client.atlas.exception.AtlasClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import java.util.UUID;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AtlasClientErrorDecoder implements ErrorDecoder {

    @SneakyThrows
    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 400 || response.status() == 404) {
            throw new ObjectMapper().readValue(response.body().asInputStream(),
                    AtlasClientException.class);
        } else {
            throw new AtlasClientException(UUID.randomUUID(), String.valueOf(response.status()),
                    response.toString());
        }
    }
}
