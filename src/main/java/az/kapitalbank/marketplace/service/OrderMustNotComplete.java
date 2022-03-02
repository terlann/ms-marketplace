package az.kapitalbank.marketplace.service;

public class OrderMustNotComplete extends RuntimeException {

    private static final String MESSAGE = "Customer not found. %s";

    public OrderMustNotComplete(String message) {
        super(String.format(MESSAGE, message));

    }
}
