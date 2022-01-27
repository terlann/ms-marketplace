package az.kapitalbank.marketplace.exception;

import java.math.BigDecimal;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LoanAmountIncorrectException extends RuntimeException {

    static final String MESSAGE = "The loan amount is incorrect. expected amount - [%s]";

    public LoanAmountIncorrectException(BigDecimal message) {
        super(String.format(MESSAGE, message));
    }
}
