package az.kapitalbank.marketplace.service;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.model.request.PurchaseCompleteRequest;
import az.kapitalbank.marketplace.client.atlas.model.request.ReversPurchaseRequest;
import az.kapitalbank.marketplace.constants.TransactionStatus;
import az.kapitalbank.marketplace.dto.DeliveryProductDto;
import az.kapitalbank.marketplace.dto.OrderProductDeliveryInfo;
import az.kapitalbank.marketplace.dto.OrderProductItem;
import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.dto.request.PurchaseRequestDto;
import az.kapitalbank.marketplace.dto.request.ReverseRequestDto;
import az.kapitalbank.marketplace.dto.response.CheckOrderResponseDto;
import az.kapitalbank.marketplace.dto.response.CreateOrderResponse;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.entity.ProductEntity;
import az.kapitalbank.marketplace.exception.LoanAmountIncorrectException;
import az.kapitalbank.marketplace.exception.OrderAlreadyScoringException;
import az.kapitalbank.marketplace.exception.OrderIsInactiveException;
import az.kapitalbank.marketplace.exception.OrderNotFoundException;
import az.kapitalbank.marketplace.mappers.CreateOrderMapper;
import az.kapitalbank.marketplace.mappers.CustomerMapper;
import az.kapitalbank.marketplace.mappers.OperationMapper;
import az.kapitalbank.marketplace.mappers.OrderMapper;
import az.kapitalbank.marketplace.messaging.sender.FraudCheckSender;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import az.kapitalbank.marketplace.repository.OperationRepository;
import az.kapitalbank.marketplace.repository.OrderRepository;
import az.kapitalbank.marketplace.utils.GenerateUtil;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import javax.transaction.Transactional;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
    AtlasClient atlasClient;
    OrderMapper orderMapper;
    OrderRepository orderRepository;

    @NonFinal
    @Value("${purchase.terminal-name}")
    String terminalName;

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequestDto request) {
        log.info("create loan process start... Request - [{}]", request);
        validateOrderAmount(request);
// TODO calculate commission for every order and set orderEntity
        CustomerEntity customerEntity = customerMapper.toCustomerEntity(request.getCustomerInfo());
        OperationEntity operationEntity = operationMapper.toOperationEntity(request);
        operationEntity.setCustomer(customerEntity);

        List<OrderEntity> orderEntities = new ArrayList<>();
        List<ProductEntity> productEntities = new ArrayList<>();
        for (OrderProductDeliveryInfo deliveryInfo : request.getDeliveryInfo()) {
            OrderEntity orderEntity = OrderEntity.builder()
                    .totalAmount(deliveryInfo.getTotalAmount())
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
        customerEntity.setOperations(Collections.singletonList(operationEntity));
        customerRepository.save(customerEntity);
        log.info("saved Customer " + customerEntity);
        var trackId = operationEntity.getId();
        /*
        FraudCheckEvent fraudCheckEvent = createOrderMapper.toOrderEvent(request);
        fraudCheckEvent.setTrackId(trackId);
        customerOrderProducer.sendMessage(fraudCheckEvent);
         */
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

    public void purchase(PurchaseRequestDto request) {
        var customerEntityOptional = customerRepository.findById(request.getCustomerId());
        if (customerEntityOptional.isPresent()) {
            var cardUUID = customerEntityOptional.get().getCardUUID();
            for (DeliveryProductDto deliveryProductDto : request.getDeliveryOrders()) {
                var optionalOrderEntity = orderRepository.findByOrderNo(deliveryProductDto.getOrderNo());
                if (optionalOrderEntity.isPresent()) {
                    var orderEntity = optionalOrderEntity.get();
                    if (!orderEntity.getOrderNo().equals(deliveryProductDto.getOrderNo()) ||
                            orderEntity.getTotalAmount().compareTo(deliveryProductDto.getOrderLastAmount()) == -1) {
                        throw new RuntimeException("Order or amount isn't equals");
                    }
                    var rrn = GenerateUtil.rrn();
                    var purchaseCompleteRequest = PurchaseCompleteRequest.builder()
                            .id(Integer.valueOf(orderEntity.getTransactionId()))
                            .uid(cardUUID)
                            .amount(deliveryProductDto.getOrderLastAmount())
                            .approvalCode(orderEntity.getApprovalCode())
                            .currency(944)
                            .description("Umico marketplace, order was delivered")//TODO text ok?
                            .rrn(rrn)
                            .terminalName(terminalName)
                            .build();

                    var purchaseResponse = atlasClient.complete(purchaseCompleteRequest);
                    orderEntity.setTransactionId(purchaseResponse.getId());
                    orderEntity.setRrn(rrn);
                    orderEntity.setTransactionStatus(TransactionStatus.COMPLETED);
                    orderRepository.save(orderEntity);
                }
            }
        } else
            throw new RuntimeException("Customer not found");
    }

    @Transactional
    public void reverse(ReverseRequestDto request) {
        customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        var orderEntity = orderRepository.findByOrderNo(request.getOrderNo())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        var reversPurchaseRequest = ReversPurchaseRequest.builder()
                .description("everything will be okay")  //TODO text ok?)
                .build();
        var reverseResponse = atlasClient.reverse(orderEntity.getTransactionId(), reversPurchaseRequest);
        orderEntity.setTransactionId(reverseResponse.getId());
        orderEntity.setTransactionStatus(TransactionStatus.REVERSED);
        orderRepository.save(orderEntity);
    }
}