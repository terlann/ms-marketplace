package az.kapitalbank.marketplace.exception.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderAlreadyDeactivatedException extends RuntimeException {

    static final String MESSAGE = "Order already have been deactivated. ete_order_id - [%s]";

    public OrderAlreadyDeactivatedException(String orderId) {
        super(String.format(MESSAGE, orderId));
    }
}
