package az.kapitalbank.marketplace.mapper;

import java.math.BigDecimal;

import az.kapitalbank.marketplace.dto.OrderProductDeliveryInfo;
import az.kapitalbank.marketplace.dto.OrderProductItem;
import az.kapitalbank.marketplace.dto.response.CheckOrderResponseDto;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.entity.ProductEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface OrderMapper {

    @Mapping(source = "id", target = "trackId")
    CheckOrderResponseDto entityToDto(OperationEntity source);

    OrderEntity toOrderEntity(OrderProductDeliveryInfo deliveryInfo, BigDecimal commission);

    ProductEntity toProductEntity(OrderProductItem orderProductItem, String orderNo);
}
