package az.kapitalbank.marketplace.service.impl;

import az.kapitalbank.marketplace.dto.response.CheckOrderResponseDto;
import az.kapitalbank.marketplace.dto.response.WrapperResponseDto;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.exception.models.OrderAlreadyScoringException;
import az.kapitalbank.marketplace.exception.models.OrderIsInactiveException;
import az.kapitalbank.marketplace.exception.models.OrderNotFindException;
import az.kapitalbank.marketplace.mappers.OrderMapper;
import az.kapitalbank.marketplace.repository.OrderRepository;
import az.kapitalbank.marketplace.service.CheckOrderService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CheckOrderServiceImpl implements CheckOrderService {

    OrderMapper orderMapper;
    OrderRepository orderRepository;

    @Override
    public ResponseEntity<WrapperResponseDto> checkOrder(String eteId) {
        var eteOrderId = eteId.trim();
        log.info("check order start... ete_order_id  - [{}]", eteOrderId);
        Optional<OrderEntity> marketplaceOrderEntity = orderRepository.findByEteOrderId(eteOrderId);
        Optional.of(marketplaceOrderEntity
                .orElseThrow(() -> new OrderNotFindException((String.format("ete_order_id - [%s]", eteOrderId)))))
                .map(OrderEntity::getIsActive)
                .filter(integer -> integer == 1)
                .orElseThrow(() -> new OrderIsInactiveException(String.format("ete_order_id - [%s]", eteOrderId)));
        if (marketplaceOrderEntity.get().getScoringStatus() != null) {
            throw new OrderAlreadyScoringException(eteOrderId);
        }
        log.info("check order find order. ete_order_id - [{}]", marketplaceOrderEntity.get().getEteOrderId());
        CheckOrderResponseDto orderResponseDto = orderMapper.entityToDto(marketplaceOrderEntity.get());
        WrapperResponseDto<Object> wrapperResponseDto = WrapperResponseDto.ofSuccess();
        wrapperResponseDto.setData(orderResponseDto);
        log.info("check order finish... ete_order_id - [{}]", eteOrderId);
        return ResponseEntity.ok(wrapperResponseDto);

    }

}
