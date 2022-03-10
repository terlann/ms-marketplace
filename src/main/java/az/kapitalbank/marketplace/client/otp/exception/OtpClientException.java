package az.kapitalbank.marketplace.client.otp.exception;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;

@Getter
@JsonIgnoreProperties(ignoreUnknown = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OtpClientException extends RuntimeException {

    String code;
    String error;
    String message;
    String detail;

    public OtpClientException(String message, String code, String error, String detail) {
        super(message);
        this.code = code;
        this.error = error;
        this.message = message;
        this.detail = detail;
    }

    public OtpClientException(String message, String detail) {
        super(message);
        this.message = message;
        this.detail = detail;
    }
}
