package az.kapitalbank.marketplace.mappers;

import java.util.List;

import az.kapitalbank.marketplace.client.telesales.model.CreateTelesalesOrderRequest;
import az.kapitalbank.marketplace.constant.ApplicationConstant;
import az.kapitalbank.marketplace.constant.FraudReason;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.mappers.qualifier.TelesalesQualifier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {TelesalesQualifier.class})
public interface TelesalesMapper {

    @Mapping(source = "operationEntity.fullName", target = "fullNameIamas")
    @Mapping(source = "operationEntity.pin", target = "pinCode")
    @Mapping(source = "operationEntity.mobileNumber", target = "phoneMob")
    @Mapping(source = "operationEntity.email", target = "email")
    @Mapping(source = "operationEntity.loanTerm", target = "duration")
    @Mapping(target = "position", constant = ApplicationConstant.UMICO_MARKETPLACE)
    @Mapping(source = "fraudReasons", target = "orderComment", qualifiedByName = "mapFraudReasons")
    CreateTelesalesOrderRequest toTelesalesOrder(OperationEntity operationEntity,
                                                 List<FraudReason> fraudReasons);
}
