package az.kapitalbank.marketplace.exception;

public class UmicoUserNotFoundException extends RuntimeException {

    private static final String MESSAGE = "Umico user not found. %s";

    public UmicoUserNotFoundException(String message) {
        super(String.format(MESSAGE, message));
    }
}
