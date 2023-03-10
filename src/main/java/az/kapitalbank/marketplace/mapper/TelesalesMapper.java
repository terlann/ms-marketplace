package az.kapitalbank.marketplace.mapper;

import static az.kapitalbank.marketplace.constant.OptimusConstant.SALES_SOURCE;

import az.kapitalbank.marketplace.client.telesales.model.CreateTelesalesOrderRequest;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.mapper.qualifier.TelesalesQualifier;
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
    @Mapping(source = "operationEntity.processStatus", target = "orderComment",
            qualifiedByName = "mapFraud")
    @Mapping(source = "operationEntity", target = "loanAmount", qualifiedByName = "mapTotalAmount")
    CreateTelesalesOrderRequest toTelesalesOrder(OperationEntity operationEntity);
}
