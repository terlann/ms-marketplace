package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OperationNotFoundException extends RuntimeException {

    static String MESSAGE = "Operation not found. %s";

    public OperationNotFoundException(String message) {
        super(String.format(MESSAGE, message));
    }
}
