package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderNotFoundException extends RuntimeException {

    static final String MESSAGE = "Order not found. order_no - %s";

    public OrderNotFoundException(String message) {
        super(String.format(MESSAGE, message));
    }
}
