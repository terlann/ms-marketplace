package az.kapitalbank.marketplace.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class AmountUtil {

    public static BigDecimal amountFormatting(BigDecimal amount) {
        return BigDecimal.valueOf(amount.doubleValue());
    }

    public static BigDecimal calculateCommissionAmount(BigDecimal amount) {
        return BigDecimal.valueOf(amount.multiply(new BigDecimal("0.03")).doubleValue());
    }

    public static BigDecimal amountRounding(BigDecimal amount) {
        return BigDecimal.valueOf(amount.setScale(2, RoundingMode.HALF_EVEN).doubleValue());
    }

    public static BigDecimal divideAmount(BigDecimal amount) {
        return BigDecimal.valueOf(amount.divide(new BigDecimal("100")).doubleValue());
    }

    public static BigDecimal calculateLoanAmount(BigDecimal amount) {
        BigDecimal totalAmount = divideAmount(amount);
        BigDecimal commissionAmount = calculateCommissionAmount(totalAmount);
        BigDecimal loanAmount = totalAmount.add(commissionAmount);
        return BigDecimal.valueOf(amountRounding(loanAmount).doubleValue());
    }
}
