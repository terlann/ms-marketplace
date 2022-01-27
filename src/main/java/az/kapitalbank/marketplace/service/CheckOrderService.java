package az.kapitalbank.marketplace.service;

import az.kapitalbank.marketplace.dto.response.CheckOrderResponseDto;
import az.kapitalbank.marketplace.exception.OrderAlreadyScoringException;
import az.kapitalbank.marketplace.exception.OrderIsInactiveException;
import az.kapitalbank.marketplace.exception.OrderNotFoundException;
import az.kapitalbank.marketplace.mappers.OrderMapper;
import az.kapitalbank.marketplace.repository.OperationRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CheckOrderService {

    OrderMapper orderMapper;
    OperationRepository operationRepository;

    //TODO Optimus call before score
    public CheckOrderResponseDto checkOrder(String eteOrderId) {
        log.info("check order start... ete_order_id  - [{}]", eteOrderId);
        var operationEntityOptional = operationRepository.findByEteOrderId(eteOrderId);

        var exceptionMessage = String.format("ete_order_id - [%s]", eteOrderId);
        var operationEntity = operationEntityOptional.orElseThrow(
                () -> new OrderNotFoundException(exceptionMessage));

        if (operationEntity.getDeletedAt() != null)
            throw new OrderIsInactiveException(exceptionMessage);

        var scoringStatus = operationEntity.getScoringStatus();
        if (scoringStatus != null)
            throw new OrderAlreadyScoringException(eteOrderId);

        CheckOrderResponseDto orderResponseDto = orderMapper.entityToDto(operationEntity);
        log.info("check order finish... ete_order_id - [{}]", eteOrderId);
        return orderResponseDto;

    }

}
