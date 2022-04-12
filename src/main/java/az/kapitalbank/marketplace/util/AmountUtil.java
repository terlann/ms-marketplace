package az.kapitalbank.marketplace.util;

import az.kapitalbank.marketplace.config.CommissionProperties;
import az.kapitalbank.marketplace.exception.UnknownLoanTerm;
import java.math.BigDecimal;
import java.math.RoundingMode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AmountUtil {

    private final CommissionProperties commissionProperties;

    public BigDecimal getCommission(BigDecimal amount, int loanTerm) {
        return amount
                .multiply(getCommissionPercent(loanTerm))
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }

    public BigDecimal getCommissionPercent(int loanTerm) {
        var percent = commissionProperties.getValues().get(loanTerm);
        if (percent == null) {
            throw new UnknownLoanTerm(loanTerm);
        }
        return percent;
    }
}
