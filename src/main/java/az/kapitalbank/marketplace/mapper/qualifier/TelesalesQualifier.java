package az.kapitalbank.marketplace.mapper.qualifier;

import static az.kapitalbank.marketplace.constant.OptimusConstant.CARD_PRODUCT_CODE;

import az.kapitalbank.marketplace.constant.FraudType;
import az.kapitalbank.marketplace.entity.OperationEntity;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;


@Component
public class TelesalesQualifier {

    @Named("mapFraudTypes")
    public String mapFraudTypes(List<FraudType> fraudTypes) {
        var frauds = fraudTypes.stream().map(Object::toString).collect(Collectors.joining(";"));
        return frauds.isEmpty() ? CARD_PRODUCT_CODE : CARD_PRODUCT_CODE + ";" + frauds;
    }

    @Named("mapTotalAmount")
    public BigDecimal mapTotalAmount(OperationEntity operationEntity) {
        return operationEntity.getTotalAmount().add(operationEntity.getCommission());
    }
}
