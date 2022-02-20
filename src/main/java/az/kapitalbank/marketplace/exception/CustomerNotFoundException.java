package az.kapitalbank.marketplace.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CustomerNotFoundException extends RuntimeException {

    private static final String MESSAGE = "Customer not found. %s";

    public CustomerNotFoundException(String message) {
        super(String.format(MESSAGE, message));
    }
}
