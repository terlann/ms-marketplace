package az.kapitalbank.marketplace.exception;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChangeScoringStatusException extends RuntimeException {

    static final String MESSAGE = "Scoring status couldn't change. ete_order_id - [%s]";

    public ChangeScoringStatusException(String eteOrderId) {
        super(String.format(MESSAGE, eteOrderId));
    }
}
