package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OperationAlreadyScoredException extends RuntimeException {

    static String MESSAGE = "Operation had already scored. %s";

    public OperationAlreadyScoredException(String message) {
        super(String.format(MESSAGE, message));
    }
}
