package az.kapitalbank.marketplace.mappers;

import java.util.List;

import az.kapitalbank.marketplace.client.telesales.model.CreateTelesalesOrderRequest;
import az.kapitalbank.marketplace.constants.ApplicationConstants;
import az.kapitalbank.marketplace.constants.FraudReason;
import az.kapitalbank.marketplace.entity.CustomerEntity;
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

    @Mapping(source = "customerEntity.fullName", target = "fullNameIamas")
    @Mapping(source = "customerEntity.pin", target = "pinCode")
    @Mapping(source = "customerEntity.mobileNumber", target = "phoneMob")
    @Mapping(source = "customerEntity.email", target = "email")
    @Mapping(source = "operationEntity.loanTerm", target = "duration")
    @Mapping(target = "position", constant = ApplicationConstants.UMICO_MARKETPLACE)
    @Mapping(source = "operationEntity.totalAmount", target = "loanAmount")
    @Mapping(source = "fraudReasons", target = "orderComment", qualifiedByName = "mapFraudReasons")
    CreateTelesalesOrderRequest toTelesalesOrder(CustomerEntity customerEntity,
                                                 OperationEntity operationEntity,
                                                 List<FraudReason> fraudReasons);

}
