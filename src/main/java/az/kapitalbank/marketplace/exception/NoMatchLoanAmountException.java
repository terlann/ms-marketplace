package az.kapitalbank.marketplace.exception;

import java.math.BigDecimal;

public class NoMatchLoanAmountException extends RuntimeException {

    private static final String MESSAGE =
            "Loan amount is not equal total order amount. loanAmount=%s , totalOrderAmount=%s";

    public NoMatchLoanAmountException(BigDecimal loanAmount, BigDecimal totalOrderAmount) {
        super(String.format(MESSAGE, loanAmount, totalOrderAmount));
    }
}
