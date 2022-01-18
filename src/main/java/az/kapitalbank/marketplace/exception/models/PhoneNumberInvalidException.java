package az.kapitalbank.marketplace.exception.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PhoneNumberInvalidException extends RuntimeException {
    public PhoneNumberInvalidException(String message) {
        super(message);
    }
}
