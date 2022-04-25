package az.kapitalbank.marketplace.exception;

public class CustomerNotFoundException extends RuntimeException {

    private static final String MESSAGE = "Customer not found.%s";

    public CustomerNotFoundException(String message) {
        super(String.format(MESSAGE, message));
    }
}