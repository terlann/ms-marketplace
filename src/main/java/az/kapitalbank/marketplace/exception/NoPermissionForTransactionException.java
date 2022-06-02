package az.kapitalbank.marketplace.exception;

public class NoPermissionForTransactionException extends RuntimeException {

    public NoPermissionForTransactionException(String message) {
        super(message);
    }
}
