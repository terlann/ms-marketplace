package az.kapitalbank.marketplace.exception;

public class NoPermissionForTransaction extends RuntimeException {

    public NoPermissionForTransaction(String message) {
        super(message);
    }
}
