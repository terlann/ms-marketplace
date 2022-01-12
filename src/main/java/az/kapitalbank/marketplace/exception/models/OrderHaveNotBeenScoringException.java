package az.kapitalbank.marketplace.exception.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderHaveNotBeenScoringException extends RuntimeException {

    static final String MESSAGE = "Order have not been scoring yet. %s";

    public OrderHaveNotBeenScoringException(String message) {
        super(String.format(MESSAGE, message));
    }
}
