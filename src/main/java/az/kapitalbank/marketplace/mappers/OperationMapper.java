package az.kapitalbank.marketplace.mappers;

import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.entity.OperationEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OperationMapper {

    @Mapping(source = "totalAmount", target = "totalAmount")
    @Mapping(source = "loanTerm", target = "loanTerm")
    @Mapping(source = "customerInfo.latitude", target = "latitude")
    @Mapping(source = "customerInfo.longitude", target = "longitude")
    @Mapping(source = "customerInfo.ip", target = "ip")
    @Mapping(source = "customerInfo.userAgent", target = "userAgent")
    @Mapping(source = "customerInfo.pin", target = "pin")
    @Mapping(source = "customerInfo.mobileNumber", target = "mobileNumber")
    OperationEntity toOperationEntity(CreateOrderRequestDto source);
}
