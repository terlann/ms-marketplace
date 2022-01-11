package az.kapitalbank.marketplace.mappers;

import az.kapitalbank.marketplace.client.telesales.model.CreateTelesalesOrderRequest;
import az.kapitalbank.marketplace.constants.ApplicationConstants;
import az.kapitalbank.marketplace.constants.FraudReason;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.mappers.qualifier.TelesalesQualifier;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {TelesalesQualifier.class})
public interface TelesalesMapper {

    @Mapping(source = "customerEntity.fullName", target = "fullNameIamas")
    @Mapping(source = "customerEntity.identityNumber", target = "pinCode")
    @Mapping(source = "customerEntity.mobileNumber", target = "phoneMob")
    @Mapping(source = "customerEntity.email", target = "email")
    @Mapping(source = "orderEntity.loanDuration", target = "duration")
    @Mapping(target = "position", constant = ApplicationConstants.UMICO_MARKETPLACE)
    @Mapping(source = "orderEntity.totalAmount", target = "loanAmount")
    @Mapping(source = "fraudReasons", target = "orderComment", qualifiedByName = "mapFraudReasons")
    CreateTelesalesOrderRequest toTelesalesOrder(CustomerEntity customerEntity,
                                                 OrderEntity orderEntity,
                                                 List<FraudReason> fraudReasons);

}
