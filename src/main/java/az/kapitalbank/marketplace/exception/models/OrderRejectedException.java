package az.kapitalbank.marketplace.exception.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderRejectedException extends RuntimeException {

    static final String MESSAGE = "This order have been reject. %s";

    public OrderRejectedException(String message) {
        super(String.format(MESSAGE, message));
    }
}
