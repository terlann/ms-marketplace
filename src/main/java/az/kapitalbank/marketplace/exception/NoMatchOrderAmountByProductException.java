package az.kapitalbank.marketplace.exception;

import java.math.BigDecimal;

public class NoMatchOrderAmountByProductException extends RuntimeException {

    private static final String MESSAGE =
            "Order amount is not equal total product amount. orderAmount=%s , productsTotalAmount=%s";

    public NoMatchOrderAmountByProductException(BigDecimal orderAmount,
                                                BigDecimal productsTotalAmount) {
        super(String.format(MESSAGE, orderAmount, productsTotalAmount));
    }
}
