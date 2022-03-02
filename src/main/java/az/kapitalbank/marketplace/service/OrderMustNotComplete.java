package az.kapitalbank.marketplace.service;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderMustNotComplete extends RuntimeException {

    static String MESSAGE = "Customer not found. %s";

    public OrderMustNotComplete(String message) {
        super(String.format(MESSAGE, message));

    }
}
