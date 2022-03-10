package az.kapitalbank.marketplace.mapper;

import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.mapper.qualifier.CreateOrderQualifier;
import az.kapitalbank.marketplace.messaging.event.FraudCheckEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {CreateOrderQualifier.class})
public interface CreateOrderMapper {

    @Mapping(source = "customerInfo.umicoUserId", target = "umicoUserId")
    @Mapping(source = "customerInfo.ip", target = "ip")
    @Mapping(source = "customerInfo.pin", target = "pin")
    @Mapping(source = "customerInfo.email", target = "email")
    @Mapping(source = "customerInfo.userAgent", target = "userAgent")
    @Mapping(source = "customerInfo.workPlace", target = "workPlace")
    @Mapping(source = "customerInfo.mobileNumber", target = "mobileNumber")
    @Mapping(source = "request.deliveryInfo", target = "deliveryAddresses",
            qualifiedByName = "mapDeliveryAddresses")
    FraudCheckEvent toOrderEvent(CreateOrderRequestDto request);
}
