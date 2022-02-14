package az.kapitalbank.marketplace.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import az.kapitalbank.marketplace.config.CommissionProperties;
import az.kapitalbank.marketplace.exception.UnknownLoanTerm;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Component;

@Component
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AmountUtil {

    static CommissionProperties commissionProperties;

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

    public static BigDecimal getCommission(BigDecimal amount, int loanTerm) {
        BigDecimal percent = commissionProperties.getValues().get(loanTerm);
        if (percent == null) {
            throw new UnknownLoanTerm(loanTerm);
        }
        return amount
                .multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
