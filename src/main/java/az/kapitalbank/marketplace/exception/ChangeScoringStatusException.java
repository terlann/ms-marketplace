package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangeScoringStatusException extends RuntimeException {

    static final String MESSAGE = "Scoring status couldn't change. telesales_order_id - [%s]";

    public ChangeScoringStatusException(String telesalesOrderId) {
        super(String.format(MESSAGE, telesalesOrderId));
    }
}
