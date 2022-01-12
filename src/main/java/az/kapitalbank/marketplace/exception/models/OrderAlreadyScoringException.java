package az.kapitalbank.marketplace.exception.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderAlreadyScoringException extends RuntimeException {

    static final String MESSAGE = "This scoring has already been scoring. ete_order_id - [%s]";

    public OrderAlreadyScoringException(String eteOrderId) {
        super(String.format(MESSAGE, eteOrderId));
    }
}
