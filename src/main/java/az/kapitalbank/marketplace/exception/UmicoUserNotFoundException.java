package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UmicoUserNotFoundException extends RuntimeException {

    static String MESSAGE = "Umico user not found. %s";

    public UmicoUserNotFoundException(String message) {
        super(String.format(MESSAGE, message));
    }
}
