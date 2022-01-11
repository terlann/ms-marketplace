package az.kapitalbank.marketplace.exception.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderNotFindException extends RuntimeException {

    static final String MESSAGE = "Order cannot find. %s";

    public OrderNotFindException(String message) {
        super(String.format(MESSAGE, message));
    }
}
