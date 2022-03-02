package az.kapitalbank.marketplace.exception;

import java.math.BigDecimal;

public class NoEnoughBalanceException extends RuntimeException {

    private static final String MESSAGE = "There is no enough amount in balance . Balance=%s";

    public NoEnoughBalanceException(BigDecimal balance) {
        super(String.format(MESSAGE, balance));
    }
}
