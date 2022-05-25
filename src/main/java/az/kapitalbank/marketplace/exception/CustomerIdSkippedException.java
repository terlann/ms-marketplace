package az.kapitalbank.marketplace.exception;

public class CustomerIdSkippedException extends RuntimeException {

    private static final String MESSAGE = "Skipped cutomer id in request : Umico user id - %s";

    public CustomerIdSkippedException(String message) {
        super(String.format(MESSAGE, message));
    }
}
