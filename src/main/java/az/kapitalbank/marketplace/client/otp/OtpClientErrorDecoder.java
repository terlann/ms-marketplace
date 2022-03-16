package az.kapitalbank.marketplace.client.otp;

import az.kapitalbank.marketplace.client.otp.exception.OtpClientException;
import az.kapitalbank.marketplace.client.otp.model.OtpClientErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.SneakyThrows;

public class OtpClientErrorDecoder implements ErrorDecoder {
    @SneakyThrows
    @Override
    public Exception decode(String methodKey, Response response) {
        if (response.status() == 400 || response.status() == 404) {
            var otpClientErrorResponse =
                    new ObjectMapper().readValue(response.body().asInputStream(),
                            OtpClientErrorResponse.class);
            throw new OtpClientException(otpClientErrorResponse.getCode(),
                    otpClientErrorResponse.getError(),
                    otpClientErrorResponse.getCode(),
                    otpClientErrorResponse.getDetail());
        } else {
            throw new OtpClientException(String.valueOf(response.status()), null,
                    response.toString(), null);
        }
    }
}
