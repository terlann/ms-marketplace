package az.kapitalbank.marketplace.mappers.qualifier;

import az.kapitalbank.marketplace.dto.OrderProductDeliveryInfo;
import az.kapitalbank.marketplace.utils.AmountUtil;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;


@Component
public class CreateOrderQualifier {

    @Named("divideAmount")
    public BigDecimal divideAmount(BigDecimal amount) {
        BigDecimal dividedAmount = AmountUtil.divideAmount(amount);
        return AmountUtil.amountRounding(dividedAmount);
    }

    @Named("calculateLoanAmount")
    public BigDecimal calculateLoanAmount(Integer amount) {
        BigDecimal totalAmount = AmountUtil.divideAmount(BigDecimal.valueOf(amount));
        BigDecimal commissionAmount = AmountUtil.calculateCommissionAmount(totalAmount);
        BigDecimal loanAmount = totalAmount.add(commissionAmount);
        return AmountUtil.amountRounding(loanAmount);
    }

    @Named("mapDeliveryAddresses")
    public Set<String> mapDeliveryAddresses(List<OrderProductDeliveryInfo> deliveryInfo) {
        return deliveryInfo
                .stream()
                .map(OrderProductDeliveryInfo::getDeliveryAddress)
                .collect(Collectors.toSet());
    }
}
