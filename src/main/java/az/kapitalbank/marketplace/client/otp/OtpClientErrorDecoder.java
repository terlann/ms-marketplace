package az.kapitalbank.marketplace.client.otp;

import az.kapitalbank.marketplace.client.otp.exception.OtpClientException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.SneakyThrows;

public class OtpClientErrorDecoder implements ErrorDecoder {
    @SneakyThrows
    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 400 || response.status() == 404) {
            throw new ObjectMapper().readValue(response.body().asInputStream(),
                    OtpClientException.class);
        } else {
            throw new OtpClientException(response.toString(), null, null, "");
        }
    }
}
