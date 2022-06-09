package az.kapitalbank.marketplace.util;

import az.kapitalbank.marketplace.config.CommissionProperties;
import az.kapitalbank.marketplace.constant.Error;
import az.kapitalbank.marketplace.exception.CommonException;
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
            throw new CommonException(Error.LOAN_TERM_NOT_FOUND,
                    "No such loan term. loanTerm - " + loanTerm);
        }
        return percent;
    }

    public BigDecimal getCommissionByPercent(BigDecimal amount, BigDecimal loanPercent) {
        return amount
                .multiply(loanPercent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
}
