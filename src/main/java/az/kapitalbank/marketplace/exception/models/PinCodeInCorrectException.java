package az.kapitalbank.marketplace.exception.models;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PinCodeInCorrectException extends RuntimeException {

    public PinCodeInCorrectException(String message) {
        super(message);
    }

}
