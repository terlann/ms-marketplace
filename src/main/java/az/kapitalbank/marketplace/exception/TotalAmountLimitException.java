package az.kapitalbank.marketplace.exception;

import java.math.BigDecimal;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TotalAmountLimitException extends RuntimeException {

    static String MESSAGE = "Purchase amount must be between 50 and 20000 in first transaction." +
            " Purchase Amount: %s";

    public TotalAmountLimitException(BigDecimal purchaseAmount) {
        super(String.format(MESSAGE, purchaseAmount));
    }
}
