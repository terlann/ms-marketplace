package az.kapitalbank.marketplace.mapper;

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
    @Mapping(source = "customerInfo.email", target = "email")
    @Mapping(source = "customerInfo.workPlace", target = "workPlace")
    @Mapping(source = "customerInfo.mobileNumber", target = "mobileNumber")
    @Mapping(source = "customerInfo.fullName", target = "fullName")
    @Mapping(source = "customerInfo.additionalPhoneNumber1", target = "additionalPhoneNumber1")
    @Mapping(source = "customerInfo.additionalPhoneNumber2", target = "additionalPhoneNumber2")
    @Mapping(source = "customerInfo.birthday", target = "birthday")
    OperationEntity toOperationEntity(CreateOrderRequestDto source);
}
