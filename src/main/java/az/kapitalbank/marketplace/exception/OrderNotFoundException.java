package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderNotFoundException extends RuntimeException {

    static String MESSAGE = "Order not found. %s";

    public OrderNotFoundException(String message) {
        super(String.format(MESSAGE, message));
    }
}
