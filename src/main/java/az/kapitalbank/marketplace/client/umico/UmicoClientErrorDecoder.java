package az.kapitalbank.marketplace.client.umico;

import az.kapitalbank.marketplace.client.umico.exception.UmicoClientException;
import az.kapitalbank.marketplace.client.umico.model.UmicoClientErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.SneakyThrows;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UmicoClientErrorDecoder implements ErrorDecoder {

    @SneakyThrows
    @Override
    public Exception decode(String methodKey, Response response) {
        if (isErrorStatus(response.status())) {
            var errorResponse =
                    new ObjectMapper().readValue(response.body().asInputStream(),
                            UmicoClientErrorResponse.class);
            return new UmicoClientException(errorResponse.getDescription());
        } else {
            throw new UmicoClientException(methodKey, response.body().toString());
        }
    }

    private boolean isErrorStatus(int httpStatus) {
        var status = String.valueOf(httpStatus);
        return status.startsWith("4") || status.startsWith("5");
    }
}
