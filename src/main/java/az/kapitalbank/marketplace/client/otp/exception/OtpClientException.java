package az.kapitalbank.marketplace.client.otp.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
public class OtpClientException extends RuntimeException {

    private final String code;
    private final String error;
    private final String message;
    private final String detail;

    public OtpClientException(String message, String code, String error, String detail) {
        super(message);
        this.code = code;
        this.error = error;
        this.message = message;
        this.detail = detail;
    }
}
