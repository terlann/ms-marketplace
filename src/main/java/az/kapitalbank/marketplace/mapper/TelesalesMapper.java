package az.kapitalbank.marketplace.mapper;

import static az.kapitalbank.marketplace.constant.OptimusConstant.CARD_PRODUCT_CODE;
import static az.kapitalbank.marketplace.constant.OptimusConstant.SALES_SOURCE;

import az.kapitalbank.marketplace.client.telesales.model.CreateTelesalesOrderRequest;
import az.kapitalbank.marketplace.constant.FraudType;
import az.kapitalbank.marketplace.dto.LeadDto;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.mapper.qualifier.TelesalesQualifier;
import az.kapitalbank.marketplace.messaging.event.FraudCheckResultEvent;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = TelesalesQualifier.class)
public interface TelesalesMapper {

    @Mapping(source = "operationEntity.fullName", target = "fullNameIamas")
    @Mapping(source = "operationEntity.pin", target = "pinCode")
    @Mapping(source = "operationEntity.mobileNumber", target = "phoneMob")
    @Mapping(source = "operationEntity.email", target = "email")
    @Mapping(source = "operationEntity.loanTerm", target = "duration")
    @Mapping(target = "position", constant = SALES_SOURCE)
    @Mapping(target = "productType", constant = CARD_PRODUCT_CODE)
    @Mapping(source = "fraudTypes", target = "orderComment", qualifiedByName = "mapFraudTypes")
    CreateTelesalesOrderRequest toTelesalesOrder(OperationEntity operationEntity,
                                                 List<FraudType> fraudTypes);

    LeadDto toLeadDto(FraudCheckResultEvent fraudCheckResultEvent);
}
