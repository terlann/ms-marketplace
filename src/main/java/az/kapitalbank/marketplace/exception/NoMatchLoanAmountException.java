package az.kapitalbank.marketplace.exception;

import java.math.BigDecimal;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NoMatchLoanAmountException extends RuntimeException {

    static String MESSAGE = "Loan amount is not equal total order amount. loanAmount=%s , totalOrderAmount=%s";

    public NoMatchLoanAmountException(BigDecimal loanAmount, BigDecimal totalOrderAmount) {
        super(String.format(MESSAGE, loanAmount, totalOrderAmount));
    }
}
