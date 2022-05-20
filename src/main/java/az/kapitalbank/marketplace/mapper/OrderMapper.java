package az.kapitalbank.marketplace.mapper;

import az.kapitalbank.marketplace.dto.OrderProductDeliveryInfo;
import az.kapitalbank.marketplace.dto.OrderProductItem;
import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.dto.response.CheckOrderResponseDto;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.entity.ProductEntity;
import az.kapitalbank.marketplace.mapper.qualifier.OrderQualifier;
import az.kapitalbank.marketplace.messaging.event.FraudCheckEvent;
import java.math.BigDecimal;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring",
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = OrderQualifier.class)
public interface OrderMapper {

    @Mapping(source = "customerInfo.umicoUserId", target = "umicoUserId")
    @Mapping(source = "customerInfo.ip", target = "ip")
    @Mapping(source = "customerInfo.pin", target = "pin")
    @Mapping(source = "customerInfo.email", target = "email")
    @Mapping(source = "customerInfo.userAgent", target = "userAgent")
    @Mapping(source = "customerInfo.workPlace", target = "workPlace")
    @Mapping(source = "customerInfo.mobileNumber", target = "mobileNumber")
    @Mapping(source = "request.deliveryInfo", target = "deliveryAddresses",
            qualifiedByName = "mapDeliveryAddresses")
    FraudCheckEvent toFraudCheckEvent(CreateOrderRequestDto request);

    @Mapping(target = "trackId", source = "id")
    @Mapping(target = "umicoUserId", source = "customer.umicoUserId")
    FraudCheckEvent toFraudCheckEvent(OperationEntity operationEntity);

    @Mapping(source = "id", target = "trackId")
    CheckOrderResponseDto toCheckOrderResponseDto(OperationEntity source);

    OrderEntity toOrderEntity(OrderProductDeliveryInfo deliveryInfo, BigDecimal commission);

    @Mapping(source = "productId", target = "productNo")
    @Mapping(source = "productAmount", target = "amount")
    @Mapping(source = "productName", target = "name")
    ProductEntity toProductEntity(OrderProductItem orderProductItem);
}
