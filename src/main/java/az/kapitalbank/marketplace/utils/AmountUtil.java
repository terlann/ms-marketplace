package az.kapitalbank.marketplace.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

import az.kapitalbank.marketplace.config.CommissionProperties;
import az.kapitalbank.marketplace.exception.UnknownLoanTerm;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AmountUtil {

    private final CommissionProperties commissionProperties;

    public BigDecimal getCommission(BigDecimal amount, int loanTerm) {
        var percent = commissionProperties.getValues().get(loanTerm);
        if (percent == null) {
            throw new UnknownLoanTerm(loanTerm);
        }
        return amount
                .multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
