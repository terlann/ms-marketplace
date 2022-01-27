package az.kapitalbank.marketplace.service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import az.kapitalbank.marketplace.dto.OrderProductDeliveryInfo;
import az.kapitalbank.marketplace.dto.OrderProductItem;
import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.dto.response.CreateOrderResponse;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.entity.ProductEntity;
import az.kapitalbank.marketplace.exception.LoanAmountIncorrectException;
import az.kapitalbank.marketplace.exception.OrderAlreadyScoringException;
import az.kapitalbank.marketplace.exception.OrderNotFoundException;
import az.kapitalbank.marketplace.mappers.CreateOrderMapper;
import az.kapitalbank.marketplace.mappers.CustomerMapper;
import az.kapitalbank.marketplace.mappers.OperationMapper;
import az.kapitalbank.marketplace.messaging.event.FraudCheckEvent;
import az.kapitalbank.marketplace.messaging.sender.FraudCheckSender;
import az.kapitalbank.marketplace.repository.CustomerRepository;
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
public class OrderService {

    OperationRepository operationRepository;
    CustomerMapper customerMapper;
    OperationMapper operationMapper;
    CreateOrderMapper createOrderMapper;
    CustomerRepository customerRepository;
    FraudCheckSender customerOrderProducer;


    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequestDto request) {
        log.info("create loan process start... Request - [{}]", request);
        validateOrderAmount(request);
// TODO calculate commission for every order and set orderEntity
        CustomerEntity customerEntity = customerMapper.toCustomerEntity(request.getCustomerInfo());
        OperationEntity operationEntity = operationMapper.toOperationEntity(request);
        customerEntity.setOperations(Collections.singletonList(operationEntity));
        operationEntity.setCustomer(customerEntity);

        List<OrderEntity> orderEntities = new ArrayList<>();
        List<ProductEntity> productEntities = new ArrayList<>();
        for (OrderProductDeliveryInfo deliveryInfo : request.getDeliveryInfo()) {
            OrderEntity orderEntity = OrderEntity.builder()
                    .orderNo(deliveryInfo.getOrderNo())
                    .deliveryAddress(deliveryInfo.getDeliveryAddress())
                    .operation(operationEntity)
                    .build();

            for (OrderProductItem orderProductItem : request.getProducts()) {
                if (deliveryInfo.getOrderNo().equals(orderProductItem.getOrderNo())) {
                    ProductEntity productEntity = ProductEntity.builder()
                            .productId(orderProductItem.getProductId())
                            .productAmount(orderProductItem.getProductAmount())
                            .productName(orderProductItem.getProductName())
                            .itemType(orderProductItem.getItemType())
                            .orderNo(orderEntity.getOrderNo())
                            .partnerCmsId(orderProductItem.getPartnerCmsId())
                            .order(orderEntity)
                            .build();
                    productEntities.add(productEntity);
                }
            }
            orderEntities.add(orderEntity);
            orderEntity.setProducts(productEntities);
        }
        operationEntity.setOrders(orderEntities);
        customerRepository.save(customerEntity);
        var trackId = operationEntity.getId();
        FraudCheckEvent fraudCheckEvent = createOrderMapper.toOrderEvent(request);
        fraudCheckEvent.setTrackId(trackId);
        customerOrderProducer.sendMessage(fraudCheckEvent);
        return CreateOrderResponse.of(trackId);
    }


    private void validateOrderAmount(CreateOrderRequestDto createOrderRequestDto) {
        var loanAmount = createOrderRequestDto.getTotalAmount();
        var totalOrderAmount = createOrderRequestDto.getDeliveryInfo().stream()
                .map(OrderProductDeliveryInfo::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalOrderAmount.compareTo(loanAmount) != 0) {
            log.error("create order total amount is incorrect . Request - [{}]," +
                            " total amount - [{}],expected amount - [{}]",
                    createOrderRequestDto,
                    loanAmount,
                    totalOrderAmount);
            throw new LoanAmountIncorrectException(totalOrderAmount);
        }
    }

    // TODO umico call this
    @Transactional
    public void deleteOrder(UUID trackId) {
        var operationEntity = operationRepository.findById(trackId);
        log.info("delete order start ... track_id - [{}]", trackId);
        operationEntity.orElseThrow(() -> new OrderNotFoundException(String.format("track_id - [%s]", trackId)));

        var operation = operationEntity.get();

        if (operation.getDeletedAt() != null)
            return;

        if (operation.getScoringStatus() != null) {
            throw new OrderAlreadyScoringException(operation.getEteOrderId());
        }

        operation.setDeletedAt(LocalDateTime.now());
        operationRepository.save(operation);
        log.info("delete operation finish ... track_id - [{}]", trackId);
    }


}