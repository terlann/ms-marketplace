package az.kapitalbank.marketplace.exception;

public class DeliveryException extends RuntimeException {

    private static final String MESSAGE = "Delivery operation was failed. %s";

    public DeliveryException(String message) {
        super(String.format(MESSAGE, message));
    }
}
