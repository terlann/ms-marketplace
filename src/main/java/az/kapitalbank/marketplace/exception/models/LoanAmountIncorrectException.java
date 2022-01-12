package az.kapitalbank.marketplace.exception.models;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoanAmountIncorrectException extends RuntimeException {

    static final String MESSAGE = "The loan amount is incorrect. expected amount - [%s]";

    public LoanAmountIncorrectException(String message) {
        super(String.format(MESSAGE, message));
    }
}
