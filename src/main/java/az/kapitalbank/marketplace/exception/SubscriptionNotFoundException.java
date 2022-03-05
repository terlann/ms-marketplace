package az.kapitalbank.marketplace.exception;

public class SubscriptionNotFoundException extends RuntimeException {
    static final String SUBSCRIPTION_NOT_FOUND = "Card UID related mobile number not found. %s";

    public SubscriptionNotFoundException(String message) {
        super(String.format(SUBSCRIPTION_NOT_FOUND, message));
    }
}
