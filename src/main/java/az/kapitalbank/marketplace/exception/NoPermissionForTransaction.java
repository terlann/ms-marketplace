package az.kapitalbank.marketplace.exception;

public class NoPermissionForTransaction extends RuntimeException {

    private static final String MESSAGE = "No Permission for REVERSE/COMPLETE. %s";

    public NoPermissionForTransaction(String message) {
        super(String.format(MESSAGE, message));

    }
}
