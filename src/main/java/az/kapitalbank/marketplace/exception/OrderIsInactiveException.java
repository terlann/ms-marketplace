package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderIsInactiveException extends RuntimeException {

    static final String MESSAGE = "Order is inactive. %s";

    public OrderIsInactiveException(String orderId) {
        super(String.format(MESSAGE, orderId));
    }
}
