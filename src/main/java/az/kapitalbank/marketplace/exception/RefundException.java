package az.kapitalbank.marketplace.exception;

public class RefundException extends RuntimeException {

    private static final String MESSAGE = "Refund operation couldn't finished. %s";

    public RefundException(String message) {
        super(String.format(MESSAGE, message));
    }
}
