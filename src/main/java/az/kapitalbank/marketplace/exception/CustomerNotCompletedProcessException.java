package az.kapitalbank.marketplace.exception;

public class CustomerNotCompletedProcessException extends RuntimeException {

    private static final String MESSAGE = "Customer has not yet completed the process. %s";

    public CustomerNotCompletedProcessException(String message) {
        super(String.format(MESSAGE, message));
    }
}