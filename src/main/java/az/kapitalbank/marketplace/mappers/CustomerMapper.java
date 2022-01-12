package az.kapitalbank.marketplace.mappers;

import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring",
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomerMapper {

    CustomerMapper INSTANCE = Mappers.getMapper(CustomerMapper.class);

    @Mapping(source = "customerDetail.userIp", target = "ip")
    @Mapping(source = "customerDetail.userAgent", target = "device")
    @Mapping(source = "customerDetail.umicoUserId", target = "umicoUserId")
    @Mapping(source = "customerInfo.pincode", target = "identityNumber")
    @Mapping(source = "customerInfo.phoneNumber", target = "mobileNumber")
    @Mapping(source = "customerInfo.fullname", target = "fullName")
    @Mapping(source = "customerInfo.email", target = "email")
    @Mapping(source = "customerInfo.workPlace", target = "employerName")
    CustomerEntity toCustomerEntity(CreateOrderRequestDto source);



}
