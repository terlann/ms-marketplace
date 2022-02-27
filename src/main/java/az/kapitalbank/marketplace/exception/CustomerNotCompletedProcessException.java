package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerNotCompletedProcessException extends RuntimeException {

    static String MESSAGE = "Customer has not yet completed the process. %s";

    public CustomerNotCompletedProcessException(String message) {
        super(String.format(MESSAGE, message));
    }
}
