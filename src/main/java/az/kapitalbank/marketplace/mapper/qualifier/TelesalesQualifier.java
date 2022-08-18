package az.kapitalbank.marketplace.mapper.qualifier;

import static az.kapitalbank.marketplace.constant.OptimusConstant.CARD_PRODUCT_CODE;

import az.kapitalbank.marketplace.entity.OperationEntity;
import java.math.BigDecimal;
import org.mapstruct.Named;
import org.springframework.stereotype.Component;


@Component
public class TelesalesQualifier {

    @Named("mapFraud")
    public String mapFraud(String processStatus) {
        return processStatus == null ? CARD_PRODUCT_CODE : CARD_PRODUCT_CODE + ";" + processStatus;
    }

    @Named("mapTotalAmount")
    public BigDecimal mapTotalAmount(OperationEntity operationEntity) {
        return operationEntity.getTotalAmount().add(operationEntity.getCommission());
    }
}
