package az.kapitalbank.marketplace.mapper.qualifier;

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
        return "BUMM" + fraudTypes.stream().map(Object::toString).collect(Collectors.joining(";"));
    }

    @Named("mapTotalAmount")
    public BigDecimal mapTotalAmount(OperationEntity operationEntity) {
        return operationEntity.getTotalAmount().add(operationEntity.getCommission());
    }
}
