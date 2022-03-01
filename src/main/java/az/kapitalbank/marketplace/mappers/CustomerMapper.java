package az.kapitalbank.marketplace.mappers;

import az.kapitalbank.marketplace.dto.request.CustomerInfo;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface CustomerMapper {

    CustomerEntity toCustomerEntity(CustomerInfo source);
}
