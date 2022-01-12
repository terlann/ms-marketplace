package az.kapitalbank.marketplace.mappers;

import az.kapitalbank.marketplace.dto.response.CheckOrderResponseDto;
import az.kapitalbank.marketplace.entity.OrderEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(source = "id", target = "trackId")
    CheckOrderResponseDto entityToDto(OrderEntity source);
}
