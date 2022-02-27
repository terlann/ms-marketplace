package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerNotFoundException extends RuntimeException {

    static String MESSAGE = "Customer not found. %s";

    public CustomerNotFoundException(String message) {
        super(String.format(MESSAGE, message));
    }
}
