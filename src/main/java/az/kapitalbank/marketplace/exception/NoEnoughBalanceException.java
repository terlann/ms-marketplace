package az.kapitalbank.marketplace.exception;

import java.math.BigDecimal;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoEnoughBalanceException extends RuntimeException {

    static String MESSAGE = "There is no enough amount in balance . Balance=%s";

    public NoEnoughBalanceException(BigDecimal balance) {
        super(String.format(MESSAGE, balance));
    }
}
