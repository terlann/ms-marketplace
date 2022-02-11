package az.kapitalbank.marketplace.exception;

import java.math.BigDecimal;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class NoEnoughBalanceException extends RuntimeException {

    static final String MESSAGE = "There is no enough amount in balance . Balance: %s";

    public NoEnoughBalanceException(BigDecimal balance) {
        super(String.format(MESSAGE, balance));
    }
}
