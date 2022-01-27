package az.kapitalbank.marketplace.service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.Optional;

import az.kapitalbank.marketplace.dto.request.DeliveryProductRequestDto;
import az.kapitalbank.marketplace.dto.response.WrapperResponseDto;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.exception.OrderNotFoundException;
import az.kapitalbank.marketplace.repository.OrderRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DeliveryService {

    OrderRepository orderRepository;

    @Transactional
    public ResponseEntity<Object> deliveryProducts(DeliveryProductRequestDto request) {
        log.info("delivery product start... Request - [{}]", request.toString());
        Optional<String> trackId = Optional.ofNullable(request.getMarketplaceTrackId());
        Optional<OrderEntity> marketplaceOrderEntityOptional;
        log.info("delivery product by track_id - [{}]", trackId.get());
        marketplaceOrderEntityOptional = orderRepository.findById(trackId.get());
        if (marketplaceOrderEntityOptional.isEmpty()) {
            throw new OrderNotFoundException(String.format("track_id  - [%s]", trackId.get()));
        }

        // TODO adp-atlas complete service
        log.info("delivery products find order. track_id - [{}]", marketplaceOrderEntityOptional.get().getId());
        var errorMessage = String.format("track_id - [%s]", trackId.get());
//        marketplaceOrderEntityOptional.map(OrderEntity::getIsActive)
//                .filter(o -> o == 1)
//                .orElseThrow(() -> new OrderIsInactiveException(errorMessage));

//        marketplaceOrderEntityOptional.map(OrderEntity::getScoringStatus)
//                .orElseThrow(() -> new OrderHaveNotBeenScoringException(errorMessage));

//        marketplaceOrderEntityOptional.map(OrderEntity::getScoringStatus)
//                .filter(o -> o == 1)
//                .orElseThrow(() -> new OrderRejectedException(errorMessage));


        marketplaceOrderEntityOptional.get()
                .getProducts()
                .forEach(orderProductEntity -> {
                    request.getProducts().forEach(deliveryProductDto -> {
                        Optional.ofNullable(orderProductEntity)
                                .filter(orderProductByOrderNo -> orderProductEntity
                                        .getOrderNo().equalsIgnoreCase(deliveryProductDto.getOrderNo()))
                                .filter(orderProductByProductId -> orderProductEntity
                                        .getProductId().equalsIgnoreCase(deliveryProductDto.getProductId()))
                                .ifPresent(orderProductsEntity -> {
                                    BigDecimal lastAmount = deliveryProductDto.getLastAmount()
                                            .divide(new BigDecimal("100"));

//                                    orderProductsEntity.setLastAmount(AmountUtil.amountFormatting(lastAmount));
//                                    orderProductsEntity.setDeliveryDate(LocalDateTime.now());
//                                    orderProductsEntity.setDeliveryStatus(1);
                                });

                    });

                });

//        if (marketplaceOrderEntityOptional.get().getShippingStatus() == null) {
//            marketplaceOrderEntityOptional.get().setShippingStatus(1);
//        }
        OrderEntity orderEntity = orderRepository.save(marketplaceOrderEntityOptional.get());

        log.info("delivery products save in db. track_id - [{}]", orderEntity.getId());
        log.info("delivery products finish..... track_id - [{}]", request.getMarketplaceTrackId());

        WrapperResponseDto<Object> response = WrapperResponseDto.ofSuccess();
        return ResponseEntity.ok(response);
    }


}