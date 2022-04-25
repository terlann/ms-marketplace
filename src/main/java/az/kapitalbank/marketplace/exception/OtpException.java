package az.kapitalbank.marketplace.exception;

import lombok.Getter;

@Getter
public class OtpException extends RuntimeException {

    public OtpException(String message) {
        super(message);
    }
}
