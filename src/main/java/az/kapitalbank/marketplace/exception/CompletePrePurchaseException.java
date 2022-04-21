package az.kapitalbank.marketplace.exception;

public class CompletePrePurchaseException extends RuntimeException {

    private static final String MESSAGE = "Complete pre purchase operation couldn't finished. %s";

    public CompletePrePurchaseException(String message) {
        super(String.format(MESSAGE, message));
    }
}
