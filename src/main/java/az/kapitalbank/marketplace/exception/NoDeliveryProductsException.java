package az.kapitalbank.marketplace.exception;

public class NoDeliveryProductsException extends RuntimeException {

    private static final String MESSAGE = "No delivery products for this order. orderNo - %s";

    public NoDeliveryProductsException(String orderNo) {
        super(String.format(MESSAGE, orderNo));
    }
}
