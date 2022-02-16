package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderAlreadyScoringException extends RuntimeException {

    static final String MESSAGE = "This scoring has already been scoring. telesales_order_id - [%s]";

    public OrderAlreadyScoringException(String telesalesOrderId) {
        super(String.format(MESSAGE, telesalesOrderId));
    }
}
