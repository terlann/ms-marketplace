package az.kapitalbank.marketplace.mappers;

import az.kapitalbank.marketplace.dto.OrderProductItem;
import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.entity.OrderProductEntity;
import az.kapitalbank.marketplace.mappers.qualifier.CreateOrderQualifier;
import az.kapitalbank.marketplace.messaging.event.FraudCheckEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

import java.util.List;

@Mapper(componentModel = "spring",
        unmappedSourcePolicy = ReportingPolicy.IGNORE,
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        uses = {CreateOrderQualifier.class})
public interface CreateOrderMapper {

    CreateOrderMapper INSTANCE = Mappers.getMapper(CreateOrderMapper.class);

    @Mapping(source = "orderProductEntityList", target = "products")
    @Mapping(source = "createOrderRequestDto.loanTerm", target = "loanDuration")
    @Mapping(source = "createOrderRequestDto.totalAmount", target = "totalAmount",
            qualifiedByName = "calculateLoanAmount")
    OrderEntity toOrderEntity(CreateOrderRequestDto createOrderRequestDto,
                              List<OrderProductEntity> orderProductEntityList);

    @Mapping(source = "productAmount", target = "productAmount", qualifiedByName = "divideAmount")
    OrderProductEntity toProductOrderEntity(OrderProductItem orderProductItem);

    @Mapping(source = "customerInfo.pincode", target = "identityNumber")
    @Mapping(source = "customerInfo.fullname", target = "fullname")
    @Mapping(source = "customerInfo.phoneNumber", target = "mobileNumber")
    @Mapping(source = "loanTerm", target = "loanTerm")
    @Mapping(source = "customerInfo.isAgreement", target = "isAgreement")
    @Mapping(source = "customerInfo.email", target = "email")
    @Mapping(source = "customerInfo.workPlace", target = "employerName")
    @Mapping(source = "request.customerDetail.umicoUserId", target = "umicoUserId")
    @Mapping(source = "request.customerDetail.userAgent", target = "device")
    @Mapping(source = "request.customerDetail.userIp", target = "ip")
    @Mapping(source = "totalAmount", target = "loanAmount", qualifiedByName = "calculateLoanAmount")
    @Mapping(source = "request.deliveryInfo", target = "deliveryAddresses", qualifiedByName = "mapDeliveryAddresses")
    FraudCheckEvent toOrderEvent(CreateOrderRequestDto request);


}
