package az.kapitalbank.marketplace.service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.model.request.PurchaseCompleteRequest;
import az.kapitalbank.marketplace.client.atlas.model.request.PurchaseRequest;
import az.kapitalbank.marketplace.client.atlas.model.request.ReversPurchaseRequest;
import az.kapitalbank.marketplace.client.atlas.model.response.PurchaseCompleteResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.ReverseResponse;
import az.kapitalbank.marketplace.config.CommissionProperties;
import az.kapitalbank.marketplace.constants.OrderStatus;
import az.kapitalbank.marketplace.constants.TransactionStatus;
import az.kapitalbank.marketplace.dto.DeliveryProductDto;
import az.kapitalbank.marketplace.dto.OrderProductDeliveryInfo;
import az.kapitalbank.marketplace.dto.OrderProductItem;
import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.dto.request.PurchaseRequestDto;
import az.kapitalbank.marketplace.dto.request.ReverseRequestDto;
import az.kapitalbank.marketplace.dto.response.CheckOrderResponseDto;
import az.kapitalbank.marketplace.dto.response.CreateOrderResponse;
import az.kapitalbank.marketplace.dto.response.UmicoPurchaseResponseDto;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.entity.ProductEntity;
import az.kapitalbank.marketplace.exception.AtlasException;
import az.kapitalbank.marketplace.exception.LoanAmountIncorrectException;
import az.kapitalbank.marketplace.exception.OrderAlreadyScoringException;
import az.kapitalbank.marketplace.exception.OrderNotFoundException;
import az.kapitalbank.marketplace.exception.TotalAmountLimitException;
import az.kapitalbank.marketplace.exception.UnknownLoanTerm;
import az.kapitalbank.marketplace.mappers.CreateOrderMapper;
import az.kapitalbank.marketplace.mappers.CustomerMapper;
import az.kapitalbank.marketplace.mappers.OperationMapper;
import az.kapitalbank.marketplace.mappers.OrderMapper;
import az.kapitalbank.marketplace.messaging.event.FraudCheckEvent;
import az.kapitalbank.marketplace.messaging.sender.FraudCheckSender;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import az.kapitalbank.marketplace.repository.OperationRepository;
import az.kapitalbank.marketplace.repository.OrderRepository;
import az.kapitalbank.marketplace.utils.GenerateUtil;
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
    CommissionProperties commissionProperties;

    @NonFinal
    @Value("${purchase.terminal-name}")
    String terminalName;

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequestDto request) {
        log.info("create loan process start... Request - [{}]", request);
        validateOrderAmount(request);
        var customerId = request.getCustomerInfo().getCustomerId();
        CustomerEntity customerEntity = null;
        if (customerId == null) {
            validatePurchaseAmountLimit(request);
            var pin = request.getCustomerInfo().getPin();
            var operationCount = operationRepository.operationCountByPinAndDecisionStatus(pin);
            if (operationCount != 0)
                throw new RuntimeException("This customer havent finished process" + pin);
            customerEntity = customerMapper.toCustomerEntity(request.getCustomerInfo());
        } else
            customerEntity = customerRepository.findById(customerId).orElseThrow(
                    () -> new RuntimeException("Customer not found : " + customerId));

        OperationEntity operationEntity = operationMapper.toOperationEntity(request);
        operationEntity.setCustomer(customerEntity);

        List<OrderEntity> orderEntities = new ArrayList<>();
        List<ProductEntity> productEntities = new ArrayList<>();
        for (OrderProductDeliveryInfo deliveryInfo : request.getDeliveryInfo()) {
            OrderEntity orderEntity = OrderEntity.builder()
                    .totalAmount(deliveryInfo.getTotalAmount())
                    .commission(getCommission(deliveryInfo.getTotalAmount(), request.getLoanTerm()))
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
        var trackId = operationEntity.getId();
        if (customerId == null) {
            FraudCheckEvent fraudCheckEvent = createOrderMapper.toOrderEvent(request);
            fraudCheckEvent.setTrackId(trackId);
            customerOrderProducer.sendMessage(fraudCheckEvent);
        } else {
            var cardUid = customerEntity.getCardUUID();
            for (OrderEntity orderEntity : orderEntities) {
                var rrn = GenerateUtil.rrn();
                var purchaseRequest = PurchaseRequest.builder()
                        .rrn(rrn)
                        .amount(orderEntity.getTotalAmount())
                        .description("purchase") //TODO text ?
                        .currency(944)
                        .terminalName(terminalName)
                        .uid(cardUid)
                        .build();
                var purchaseResponse = atlasClient.purchase(purchaseRequest);
                orderEntity.setRrn(rrn);
                orderEntity.setTransactionId(purchaseResponse.getId());
                orderEntity.setApprovalCode(purchaseResponse.getApprovalCode());
                orderEntity.setTransactionStatus(TransactionStatus.PURCHASE);
                orderEntities.add(orderEntity);
            }
        }
        customerRepository.save(customerEntity);
        return CreateOrderResponse.of(trackId);
    }

    private BigDecimal getCommission(BigDecimal orderAmount, int loanTerm) {
        BigDecimal percent = commissionProperties.getValues().get(loanTerm);
        if (percent == null) {
            throw new UnknownLoanTerm(loanTerm);
        }
        return orderAmount
                .multiply(percent)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
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

    private void validatePurchaseAmountLimit(CreateOrderRequestDto request) {
        BigDecimal totalAmount = request.getTotalAmount();
        BigDecimal totalCommission = BigDecimal.ZERO;

        for (var order : request.getDeliveryInfo()) {
            BigDecimal commission = getCommission(order.getTotalAmount(), request.getLoanTerm());
            totalCommission = totalCommission.add(commission);
        }
        var purchaseAmount = totalAmount.add(totalCommission);

        var minLimit = BigDecimal.valueOf(50);
        var maxLimit = BigDecimal.valueOf(20000);

        if (purchaseAmount.compareTo(minLimit) < 0 || purchaseAmount.compareTo(maxLimit) > 0) {
            throw new TotalAmountLimitException(String.valueOf(totalAmount), String.valueOf(totalCommission));
        }
    }

    //TODO Optimus call before score
    public CheckOrderResponseDto checkOrder(String telesalesOrderId) {
        log.info("check order start... telesales_order_id  - [{}]", telesalesOrderId);
        var operationEntityOptional = operationRepository.findByTelesalesOrderId(telesalesOrderId);

        var exceptionMessage = String.format("telesales_order_id - [%s]", telesalesOrderId);
        var operationEntity = operationEntityOptional.orElseThrow(
                () -> new OrderNotFoundException(exceptionMessage));

        var scoringStatus = operationEntity.getScoringStatus();
        if (scoringStatus != null)
            throw new OrderAlreadyScoringException(telesalesOrderId);

        CheckOrderResponseDto orderResponseDto = orderMapper.entityToDto(operationEntity);
        log.info("check order finish... telesales_order_id - [{}]", telesalesOrderId);
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
            throw new OrderAlreadyScoringException(operation.getTelesalesOrderId());
        }

        operation.setDeletedAt(LocalDateTime.now());
        operationRepository.save(operation);
        log.info("delete operation finish ... track_id - [{}]", trackId);
    }

    public List<UmicoPurchaseResponseDto> purchase(PurchaseRequestDto request) {
        var customerEntityOptional = customerRepository.findById(request.getCustomerId());
        UmicoPurchaseResponseDto umicoPurchaseResponseDto = null;
        List<UmicoPurchaseResponseDto> umicoPurchaseList = new ArrayList<>();
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
                    var commision = orderEntity.getCommission();
                    var amount = orderEntity.getTotalAmount();
                    var totalPayment = commision.add(amount);
                    var rrn = GenerateUtil.rrn();
                    var purchaseCompleteRequest = PurchaseCompleteRequest.builder()
                            .id(Integer.valueOf(orderEntity.getTransactionId()))
                            .uid(cardUUID)
                            .amount(totalPayment)
                            .approvalCode(orderEntity.getApprovalCode())
                            .currency(944)
                            .description("Umico marketplace, order was delivered")//TODO text ok?
                            .rrn(rrn)
                            .terminalName(terminalName)
                            .build();

                    PurchaseCompleteResponse purchaseResponse = null;
                    try {
                        purchaseResponse = atlasClient.complete(purchaseCompleteRequest);
                        orderEntity.setTransactionId(purchaseResponse.getId());
                        orderEntity.setRrn(rrn);
                        orderEntity.setTransactionStatus(TransactionStatus.COMPLETED);
                        umicoPurchaseResponseDto.setStatus(OrderStatus.SUCCESS);
                    } catch (AtlasException atlasException) {
                        umicoPurchaseResponseDto.setStatus(OrderStatus.FAIL);
                        orderEntity.setTransactionStatus(TransactionStatus.FAIL_COMPLETED);
                    }
                    umicoPurchaseResponseDto.setOrderNo(orderEntity.getOrderNo());
                    orderRepository.save(orderEntity);
                }
                umicoPurchaseList.add(umicoPurchaseResponseDto);
            }
        } else {
            throw new RuntimeException("Customer not found");
        }
        return umicoPurchaseList;
    }

    @Transactional
    public UmicoPurchaseResponseDto reverse(ReverseRequestDto request) {

        UmicoPurchaseResponseDto purchaseResponseForUmico = null;
        customerRepository.findById(request.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Customer not found"));

        var orderEntity = orderRepository.findByOrderNo(request.getOrderNo())
                .orElseThrow(() -> new RuntimeException("Order not found"));

        var reversPurchaseRequest = ReversPurchaseRequest.builder()
                .description("everything will be okay")  //TODO text ok?)
                .build();
        ReverseResponse reverseResponse = null;
        try {
            reverseResponse = atlasClient.reverse(orderEntity.getTransactionId(), reversPurchaseRequest);
            purchaseResponseForUmico.setStatus(OrderStatus.SUCCESS);
        } catch (AtlasException atlasException) {
            purchaseResponseForUmico.setStatus(OrderStatus.FAIL);
        }
        purchaseResponseForUmico.setOrderNo(orderEntity.getOrderNo());
        orderEntity.setTransactionId(reverseResponse.getId());
        orderEntity.setTransactionStatus(TransactionStatus.REVERSED);
        orderRepository.save(orderEntity);
        return purchaseResponseForUmico;
    }
}