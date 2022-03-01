package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderNotLinkedToCustomer extends RuntimeException {

    static final String MESSAGE = "Order is not linked to customer. %s";

    public OrderNotLinkedToCustomer(String message) {
        super(String.format(MESSAGE, message));
    }
}
