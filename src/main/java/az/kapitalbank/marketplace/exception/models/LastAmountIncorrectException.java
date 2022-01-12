package az.kapitalbank.marketplace.exception.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LastAmountIncorrectException extends RuntimeException {

    static final String MESSAGE = "The price of product  cannot increase. track_id - [%s]";

    public LastAmountIncorrectException(String message) {
        super(String.format(MESSAGE, message));
    }
}
