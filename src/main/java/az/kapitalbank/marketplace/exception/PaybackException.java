package az.kapitalbank.marketplace.exception;

public class PaybackException extends RuntimeException {

    private static final String MESSAGE = "Payback operation was failed. orderNo - %s";

    public PaybackException(String orderNo) {
        super(String.format(MESSAGE, orderNo));
    }
}
