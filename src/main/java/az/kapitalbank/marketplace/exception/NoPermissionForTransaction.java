package az.kapitalbank.marketplace.exception;

public class NoPermissionForTransaction extends RuntimeException {

    private static final String MESSAGE = "No Permission for REFUND/COMPLETE. %s";

    public NoPermissionForTransaction(String message) {
        super(String.format(MESSAGE, message));

    }
}
