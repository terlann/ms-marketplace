package az.kapitalbank.marketplace.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PinNotFoundException extends RuntimeException {

    public PinNotFoundException(String message) {
        super(message);
    }
}
