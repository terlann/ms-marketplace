package az.kapitalbank.marketplace.exception;

import java.math.BigDecimal;

public class TotalAmountLimitException extends RuntimeException {

    private static final String MESSAGE = "Purchase amount must be between 50 and 20000 in first transaction." +
            " Purchase Amount: %s";

    public TotalAmountLimitException(BigDecimal purchaseAmount) {
        super(String.format(MESSAGE, purchaseAmount));
    }
}
