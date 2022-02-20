package az.kapitalbank.marketplace.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import az.kapitalbank.marketplace.config.CommissionProperties;
import az.kapitalbank.marketplace.exception.UnknownLoanTerm;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AmountUtil {

    private static CommissionProperties commissionProperties;

    @Autowired
    public AmountUtil(CommissionProperties commissionProperties) {
        AmountUtil.commissionProperties = commissionProperties;
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
