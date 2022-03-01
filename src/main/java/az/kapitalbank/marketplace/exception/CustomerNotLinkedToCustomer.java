package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerNotLinkedToCustomer extends RuntimeException {

    static final String MESSAGE = "Customer is not linked to customer. %s";

    public CustomerNotLinkedToCustomer(String message) {
        super(String.format(MESSAGE, message));
    }
}
