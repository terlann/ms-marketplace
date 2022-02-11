package az.kapitalbank.marketplace.exception;

public class AtlasException extends RuntimeException {

    static final String MESSAGE = "Atlas exception.method_key - %s, Response - %s";

    public AtlasException(String message, String response) {
        super(String.format(MESSAGE, message, response));
    }
}
