package az.kapitalbank.marketplace.exception;

public class OrderNotFoundException extends RuntimeException {

    private static final String MESSAGE = "Order not found. %s";

    public OrderNotFoundException(String message) {
        super(String.format(MESSAGE, message));
    }
}
