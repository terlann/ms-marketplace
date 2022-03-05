package az.kapitalbank.marketplace.exception;

public class OperationNotFoundException extends RuntimeException {

    private static final String MESSAGE = "Operation not found. %s";

    public OperationNotFoundException(String message) {
        super(String.format(MESSAGE, message));
    }
}
