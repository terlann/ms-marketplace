package az.kapitalbank.marketplace.service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.model.request.PurchaseRequest;
import az.kapitalbank.marketplace.constants.ErrorCode;
import az.kapitalbank.marketplace.constants.OrderStatus;
import az.kapitalbank.marketplace.constants.TransactionStatus;
import az.kapitalbank.marketplace.constants.UmicoDecisionStatus;
import az.kapitalbank.marketplace.dto.DeliveryProductDto;
import az.kapitalbank.marketplace.dto.OrderProductDeliveryInfo;
import az.kapitalbank.marketplace.dto.OrderProductItem;
import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.dto.request.PurchaseRequestDto;
import az.kapitalbank.marketplace.dto.request.ReverseRequestDto;
import az.kapitalbank.marketplace.dto.response.CheckOrderResponseDto;
import az.kapitalbank.marketplace.dto.response.CreateOrderResponse;
import az.kapitalbank.marketplace.dto.response.PurchaseResponseDto;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.entity.ProductEntity;
import az.kapitalbank.marketplace.exception.LoanAmountIncorrectException;
import az.kapitalbank.marketplace.exception.MarketplaceException;
import az.kapitalbank.marketplace.exception.NoEnoughBalanceException;
import az.kapitalbank.marketplace.exception.OrderAlreadyScoringException;
import az.kapitalbank.marketplace.exception.OrderNotFoundException;
import az.kapitalbank.marketplace.exception.TotalAmountLimitException;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import static az.kapitalbank.marketplace.utils.AmountUtil.getCommission;

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

    public static BigDecimal available = new BigDecimal("900.45");
    public static BigDecimal overdraft = new BigDecimal("5000");

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequestDto request) {
        log.info("create loan process start... Request - [{}]", request);
        validateOrderAmount(request);
        var customerId = request.getCustomerInfo().getCustomerId();
        CustomerEntity customerEntity = null;
        if (customerId == null) {
            validatePurchaseAmountLimit(request);
            var customerByUmicoUserId =
                    customerRepository.findByUmicoUserId(request.getCustomerInfo().getUmicoUserId());
            if (customerByUmicoUserId.isPresent()) {
                customerEntity = customerByUmicoUserId.get();
            } else {
                customerEntity = customerMapper.toCustomerEntity(request.getCustomerInfo());
                customerEntity = customerRepository.save(customerEntity);
            }
        } else {
            customerEntity = customerRepository.findById(customerId).orElseThrow(
                    () -> new MarketplaceException("Customer not found : " + customerId,
                            ErrorCode.CUSTOMER_NOT_FOUND, HttpStatus.NOT_FOUND));
            var pendingCustomer = operationRepository.countByCustomerAndUmicoDecisionStatusIn(customerEntity,
                    List.of(UmicoDecisionStatus.PENDING, UmicoDecisionStatus.PREAPPROVED));
            if (pendingCustomer > 0)
                throw new MarketplaceException("Customer is already in progress: Customerid: " + customerId,
                        ErrorCode.CUSTOMER_IS_IN_PROGRESS, HttpStatus.BAD_REQUEST);

            validateCustomerBalance(request, customerEntity.getCardUUID());
        }
        OperationEntity operationEntity = operationMapper.toOperationEntity(request);
        operationEntity.setCustomer(customerEntity);

        var operationCommission = BigDecimal.ZERO;
        List<OrderEntity> orderEntities = new ArrayList<>();
        List<ProductEntity> productEntities = new ArrayList<>();
        for (OrderProductDeliveryInfo deliveryInfo : request.getDeliveryInfo()) {
            var commission = getCommission(deliveryInfo.getTotalAmount(), request.getLoanTerm());
            operationCommission = operationCommission.add(commission);
            OrderEntity orderEntity = OrderEntity.builder()
                    .totalAmount(deliveryInfo.getTotalAmount())
                    .commission(commission)
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
        operationEntity.setCommission(operationCommission);
        operationEntity.setOrders(orderEntities);
        operationEntity = operationRepository.save(operationEntity);
        customerEntity.getOperations().add(operationEntity);
        var trackId = operationEntity.getId();
        var approvedCustomerCount = operationRepository
                .countByCustomerAndUmicoDecisionStatus(customerEntity, UmicoDecisionStatus.APPROVED);

        if (customerId != null && approvedCustomerCount > 0) {
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
        } else {
            FraudCheckEvent fraudCheckEvent = createOrderMapper.toOrderEvent(request);
            fraudCheckEvent.setTrackId(trackId);
            customerOrderProducer.sendMessage(fraudCheckEvent);
        }
        return CreateOrderResponse.of(trackId);
    }

    private void validateCustomerBalance(CreateOrderRequestDto request, String cardUUId) {
        var purchaseAmount = getPurchaseAmount(request);
        var availableBalance = getAvailableBalance(cardUUId);
        if (purchaseAmount.compareTo(availableBalance) > 0) {
            throw new NoEnoughBalanceException(availableBalance);
        }
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
        var purchaseAmount = getPurchaseAmount(request);
        var minLimit = BigDecimal.valueOf(50);
        var maxLimit = BigDecimal.valueOf(20000);

        if (purchaseAmount.compareTo(minLimit) < 0 || purchaseAmount.compareTo(maxLimit) > 0) {
            throw new TotalAmountLimitException(String.valueOf(purchaseAmount));
        }
    }

    private BigDecimal getPurchaseAmount(CreateOrderRequestDto request) {
        BigDecimal totalAmount = request.getTotalAmount();
        BigDecimal totalCommission = BigDecimal.ZERO;

        for (var order : request.getDeliveryInfo()) {
            BigDecimal commission = getCommission(order.getTotalAmount(), request.getLoanTerm());
            totalCommission = totalCommission.add(commission);
        }
        return totalAmount.add(totalCommission);
    }

    public BigDecimal getAvailableBalance(String cardUUID) {
        var balanceResponse = atlasClient.balance(cardUUID);
        return balanceResponse.getAvailableBalance();
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

    // fin approve, fin reject
    public List<PurchaseResponseDto> purchase(PurchaseRequestDto request) {
        var customerEntityOptional = customerRepository.findById(request.getCustomerId());


        List<PurchaseResponseDto> umicoPurchaseList = new ArrayList<>();

        if (customerEntityOptional.isPresent()) {
//            var cardUUID = customerEntityOptional.get().getCardUUID();
            for (DeliveryProductDto deliveryProductDto : request.getDeliveryOrders()) {
                PurchaseResponseDto purchaseResponseDto = new PurchaseResponseDto();
                purchaseResponseDto.setOrderNo(deliveryProductDto.getOrderNo());
                purchaseResponseDto.setStatus(OrderStatus.SUCCESS);
                available = available.subtract(deliveryProductDto.getOrderLastAmount());
                umicoPurchaseList.add(purchaseResponseDto);

//                var optionalOrderEntity = orderRepository.findByOrderNo(deliveryProductDto.getOrderNo());
//
//                optionalOrderEntity.get().se
//
//                if (optionalOrderEntity.isPresent()) {
//                    var orderEntity = optionalOrderEntity.get();
////                    if (!orderEntity.getOrderNo().equals(deliveryProductDto.getOrderNo()) ||
////                            orderEntity.getTotalAmount().compareTo(deliveryProductDto.getOrderLastAmount()) == -1) {
////                        throw new RuntimeException("Order or amount isn't equals");
////                    }
//                    var commision = orderEntity.getCommission();
//                    var amount = orderEntity.getTotalAmount();
//                    var totalPayment = commision.add(amount);
//                    var rrn = GenerateUtil.rrn();
//                    var purchaseCompleteRequest = PurchaseCompleteRequest.builder()
//                            .id(Integer.valueOf(orderEntity.getTransactionId()))
//                            .uid(cardUUID)
//                            .amount(totalPayment)
//                            .approvalCode(orderEntity.getApprovalCode())
//                            .currency(944)
//                            .description("Umico marketplace, order was delivered")//TODO text ok?
//                            .rrn(rrn)
//                            .terminalName(terminalName)
//                            .build();
//
//                    PurchaseCompleteResponse purchaseResponse = null;
//                    try {
//                        purchaseResponse = atlasClient.complete(purchaseCompleteRequest);
//                        orderEntity.setTransactionId(purchaseResponse.getId());
//                        orderEntity.setRrn(rrn);
//                        orderEntity.setTransactionStatus(TransactionStatus.COMPLETED);
//                        purchaseResponseDto.setStatus(OrderStatus.SUCCESS);
//                    } catch (AtlasException atlasException) {
//                        purchaseResponseDto.setStatus(OrderStatus.FAIL);
//                        orderEntity.setTransactionStatus(TransactionStatus.FAIL_COMPLETED);
//                    }
//                    purchaseResponseDto.setOrderNo(orderEntity.getOrderNo());
//                    orderRepository.save(orderEntity);
//                }
            }
        } else {
            throw new RuntimeException("Customer not found");
        }
        return umicoPurchaseList;
    }

    @Transactional
    public PurchaseResponseDto reverse(ReverseRequestDto request) {

        PurchaseResponseDto purchaseResponse = new PurchaseResponseDto();
        available = available.subtract(request.getOrderAmount());
        purchaseResponse.setStatus(OrderStatus.SUCCESS);
        purchaseResponse.setOrderNo(request.getOrderNo());


//        customerRepository.findById(request.getCustomerId())
//                .orElseThrow(() -> new RuntimeException("Customer not found"));
//
//        var orderEntity = orderRepository.findByOrderNo(request.getOrderNo())
//                .orElseThrow(() -> new RuntimeException("Order not found"));
//
//        var reversPurchaseRequest = ReversPurchaseRequest.builder()
//                .description("everything will be okay")  //TODO text ok?)
//                .build();
//        ReverseResponse reverseResponse = null;
//        try {
//            reverseResponse = atlasClient.reverse(orderEntity.getTransactionId(), reversPurchaseRequest);
//            purchaseResponse.setStatus(OrderStatus.SUCCESS);
//            orderEntity.setTransactionId(reverseResponse.getId());
//            orderEntity.setTransactionStatus(TransactionStatus.REVERSED);
//        } catch (AtlasException atlasException) {
//            purchaseResponse.setStatus(OrderStatus.FAIL);
//            orderEntity.setTransactionStatus(TransactionStatus.FAIL_PURCHASE);
//        }
//        purchaseResponse.setOrderNo(orderEntity.getOrderNo());
//        orderRepository.save(orderEntity);
        return purchaseResponse;
    }

}