package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constant.AtlasConstant.COMPLETE_PRE_PURCHASE_DESCRIPTION;
import static az.kapitalbank.marketplace.constant.AtlasConstant.COMPLETE_PRE_PURCHASE_FOR_REFUND_DESCRIPTION;
import static az.kapitalbank.marketplace.constant.AtlasConstant.PRE_PURCHASE_DESCRIPTION;
import static az.kapitalbank.marketplace.constant.CommonConstant.CUSTOMER_ID_LOG;
import static az.kapitalbank.marketplace.constant.CommonConstant.ORDER_NO_LOG;
import static az.kapitalbank.marketplace.constant.CommonConstant.TELESALES_ORDER_ID_LOG;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.exception.AtlasClientException;
import az.kapitalbank.marketplace.client.atlas.model.request.CompletePrePurchaseRequest;
import az.kapitalbank.marketplace.client.atlas.model.request.PrePurchaseRequest;
import az.kapitalbank.marketplace.client.atlas.model.request.RefundRequest;
import az.kapitalbank.marketplace.constant.AccountStatus;
import az.kapitalbank.marketplace.constant.OperationRejectReason;
import az.kapitalbank.marketplace.constant.ResultType;
import az.kapitalbank.marketplace.constant.ScoringStatus;
import az.kapitalbank.marketplace.constant.TransactionStatus;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.dto.OrderProductDeliveryInfo;
import az.kapitalbank.marketplace.dto.OrderProductItem;
import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.dto.request.PurchaseRequestDto;
import az.kapitalbank.marketplace.dto.request.RefundRequestDto;
import az.kapitalbank.marketplace.dto.request.TelesalesResultRequestDto;
import az.kapitalbank.marketplace.dto.response.CheckOrderResponseDto;
import az.kapitalbank.marketplace.dto.response.CreateOrderResponse;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.entity.ProductEntity;
import az.kapitalbank.marketplace.exception.CustomerNotCompletedProcessException;
import az.kapitalbank.marketplace.exception.CustomerNotFoundException;
import az.kapitalbank.marketplace.exception.NoEnoughBalanceException;
import az.kapitalbank.marketplace.exception.NoMatchLoanAmountByOrderException;
import az.kapitalbank.marketplace.exception.NoMatchOrderAmountByProductException;
import az.kapitalbank.marketplace.exception.NoPermissionForTransaction;
import az.kapitalbank.marketplace.exception.OperationAlreadyScoredException;
import az.kapitalbank.marketplace.exception.OperationNotFoundException;
import az.kapitalbank.marketplace.exception.OrderNotFoundException;
import az.kapitalbank.marketplace.exception.OrderNotLinkedToCustomer;
import az.kapitalbank.marketplace.exception.ProductNotLinkedToOrder;
import az.kapitalbank.marketplace.exception.TotalAmountLimitException;
import az.kapitalbank.marketplace.exception.UniqueAdditionalNumberException;
import az.kapitalbank.marketplace.mapper.CustomerMapper;
import az.kapitalbank.marketplace.mapper.OperationMapper;
import az.kapitalbank.marketplace.mapper.OrderMapper;
import az.kapitalbank.marketplace.messaging.publisher.FraudCheckPublisher;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import az.kapitalbank.marketplace.repository.OperationRepository;
import az.kapitalbank.marketplace.repository.OrderRepository;
import az.kapitalbank.marketplace.util.AmountUtil;
import az.kapitalbank.marketplace.util.GenerateUtil;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.transaction.Transactional;
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

    AmountUtil amountUtil;
    OrderMapper orderMapper;
    AtlasClient atlasClient;
    UmicoService umicoService;
    CustomerMapper customerMapper;
    OrderRepository orderRepository;
    OperationMapper operationMapper;
    CustomerRepository customerRepository;
    FraudCheckPublisher fraudCheckPublisher;
    OperationRepository operationRepository;

    @Transactional
    public void telesalesResult(TelesalesResultRequestDto request) {
        var telesalesOrderId = request.getTelesalesOrderId().trim();
        log.info("Telesales result is started... request - {}", request);
        var operationEntity = operationRepository.findByTelesalesOrderId(telesalesOrderId)
                .orElseThrow(() -> new OperationNotFoundException(
                        TELESALES_ORDER_ID_LOG + telesalesOrderId));
        if (operationEntity.getScoringStatus() != null) {
            throw new OperationAlreadyScoredException(TELESALES_ORDER_ID_LOG + telesalesOrderId);
        }
        if (request.getScoringStatus() == ScoringStatus.APPROVED) {
            telesalesResultApproveProcess(request, operationEntity);
        } else {
            var umicoDecisionStatus = umicoService.sendRejectedDecision(operationEntity.getId());
            operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
            operationEntity.setOperationRejectReason(OperationRejectReason.TELESALES);
            operationEntity.setScoringStatus(ScoringStatus.REJECTED);
        }
        operationEntity.setScoringDate(LocalDateTime.now());
        operationRepository.save(operationEntity);
    }

    private void telesalesResultApproveProcess(TelesalesResultRequestDto request,
                                               OperationEntity operationEntity) {
        operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.APPROVED);
        operationEntity.setScoringStatus(ScoringStatus.APPROVED);
        operationEntity.setLoanContractStartDate(request.getLoanContractStartDate());
        operationEntity.setLoanContractEndDate(request.getLoanContractEndDate());
        operationEntity.setScoredAmount(request.getScoredAmount());
        var customerEntity = operationEntity.getCustomer();
        var cardId = request.getUid();
        customerEntity.setCardId(cardId);
        var lastTempAmount = prePurchaseOrders(operationEntity, cardId);
        if (lastTempAmount.compareTo(BigDecimal.ZERO) == 0) {
            umicoService.sendPrePurchaseResult(operationEntity.getId());
        }
    }

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequestDto request) {
        log.info("Create order process is started : request - {}", request);
        validateTotalOrderAmount(request);
        var customerId = request.getCustomerInfo().getCustomerId();
        CustomerEntity customerEntity = getCustomerEntity(request, customerId);
        OperationEntity operationEntity = saveOperationEntity(request, customerEntity);
        var trackId = operationEntity.getId();
        if (customerId == null && customerEntity.getCardId() == null) {
            log.info("First transaction process is started : customerId - {}, trackId - {}",
                    customerEntity.getId(), trackId);
            var fraudCheckEvent = orderMapper.toOrderEvent(request);
            fraudCheckEvent.setTrackId(trackId);
            fraudCheckPublisher.sendEvent(fraudCheckEvent);
        }
        log.info("Create order was finished : trackId - {}", trackId);
        return CreateOrderResponse.of(trackId);
    }

    private OperationEntity saveOperationEntity(CreateOrderRequestDto request,
                                                CustomerEntity customerEntity) {
        var operationEntity = operationMapper.toOperationEntity(request);
        operationEntity.setCustomer(customerEntity);
        var operationCommission = BigDecimal.ZERO;
        var orderEntities = new ArrayList<OrderEntity>();
        var productEntities = new ArrayList<ProductEntity>();
        for (OrderProductDeliveryInfo deliveryInfo : request.getDeliveryInfo()) {
            var commission =
                    amountUtil.getCommission(deliveryInfo.getTotalAmount(), request.getLoanTerm());
            operationCommission = operationCommission.add(commission);
            var orderEntity = orderMapper.toOrderEntity(deliveryInfo, commission);
            orderEntity.setOperation(operationEntity);
            for (var orderProductItem : request.getProducts()) {
                if (deliveryInfo.getOrderNo().equals(orderProductItem.getOrderNo())) {
                    var productEntity = orderMapper.toProductEntity(orderProductItem);
                    productEntity.setOrder(orderEntity);
                    productEntities.add(productEntity);
                }
            }
            orderEntities.add(orderEntity);
            orderEntity.setProducts(productEntities);
        }
        operationEntity.setLoanPercent(amountUtil.getCommissionPercent(request.getLoanTerm()));
        operationEntity.setCommission(operationCommission);
        operationEntity.setOrders(orderEntities);
        operationEntity = operationRepository.save(operationEntity);
        return operationEntity;
    }

    private CustomerEntity getCustomerEntity(CreateOrderRequestDto request, UUID customerId) {
        CustomerEntity customerEntity;
        if (customerId == null) {
            log.info("First order create process is started...");
            checkAdditionalPhoneNumbersEquals(request);
            validatePurchaseAmountLimit(request);
            var customerByUmicoUserId = customerRepository.findByUmicoUserId(
                    request.getCustomerInfo().getUmicoUserId());
            if (customerByUmicoUserId.isPresent()) {
                customerEntity = customerByUmicoUserId.get();
                var isExistsCustomerByDecisionStatus =
                        operationRepository.existsByCustomerAndUmicoDecisionStatusIn(customerEntity,
                                List.of(null, UmicoDecisionStatus.PENDING,
                                        UmicoDecisionStatus.PREAPPROVED));
                if (isExistsCustomerByDecisionStatus) {
                    throw new CustomerNotCompletedProcessException(CUSTOMER_ID_LOG + customerId);
                }
            } else {
                customerEntity = customerMapper.toCustomerEntity(request.getCustomerInfo());
                customerEntity = customerRepository.save(customerEntity);
                log.info("New customer was created. customerId" + customerEntity.getId());
            }
        } else {
            customerEntity = customerRepository.findById(customerId)
                    .orElseThrow(() -> new CustomerNotFoundException(CUSTOMER_ID_LOG + customerId));
            validateCustomerBalance(request, customerEntity.getCardId());
        }
        return customerEntity;
    }

    private void checkAdditionalPhoneNumbersEquals(CreateOrderRequestDto request) {
        var firstPhoneNumber = request.getCustomerInfo().getAdditionalPhoneNumber1();
        var secondPhoneNumber = request.getCustomerInfo().getAdditionalPhoneNumber2();
        if (firstPhoneNumber.equals(secondPhoneNumber)) {
            throw new UniqueAdditionalNumberException(firstPhoneNumber, secondPhoneNumber);
        }
    }

    public CheckOrderResponseDto checkOrder(String telesalesOrderId) {
        log.info("Check order is started : telesalesOrderId  - {}", telesalesOrderId);
        var operationEntityOptional = operationRepository.findByTelesalesOrderId(telesalesOrderId);
        var operationEntity = operationEntityOptional.orElseThrow(
                () -> new OperationNotFoundException(TELESALES_ORDER_ID_LOG + telesalesOrderId));
        var scoringStatus = operationEntity.getScoringStatus();
        if (scoringStatus != null) {
            throw new OperationAlreadyScoredException(TELESALES_ORDER_ID_LOG + telesalesOrderId);
        }
        var orderResponseDto = orderMapper.entityToDto(operationEntity);
        log.info("Check order was finished : telesalesOrderId - {}", telesalesOrderId);
        return orderResponseDto;
    }

    @Transactional(dontRollbackOn = AtlasClientException.class)
    public void purchase(PurchaseRequestDto request) {
        log.info("Complete pre purchase process is started : request - {}", request);
        var orderEntity = orderRepository.findByOrderNo(request.getOrderNo())
                .orElseThrow(() -> new OrderNotFoundException("orderNo - " + request.getOrderNo()));
        var transactionStatus = orderEntity.getTransactionStatus();
        var productEntities = orderEntity.getProducts();
        verifyProductIdIsLinkedToOrderNo(request, productEntities);
        if (transactionStatus == TransactionStatus.PRE_PURCHASE
                || transactionStatus == TransactionStatus.FAIL_IN_COMPLETE_PRE_PURCHASE) {
            var deliveryAmount = getDeliveryAmount(request, productEntities);
            if (deliveryAmount.compareTo(BigDecimal.ZERO) > 0) {
                var purchaseCompleteRequest =
                        getCompletePrePurchaseRequest(orderEntity, deliveryAmount);
                completePrePurchaseOrder(orderEntity, purchaseCompleteRequest);
            }
        } else {
            log.error(
                    "No Permission for complete pre purchase. orderNo - {}, transactionStatus -{}",
                    orderEntity.getOrderNo(), orderEntity.getTransactionStatus());
            throw new NoPermissionForTransaction(
                    ORDER_NO_LOG + orderEntity.getId() + " transactionStatus - "
                            + transactionStatus);
        }
        log.info("Complete pre purchase process was finished : trackId - {}, customerId - {}",
                request.getTrackId(), request.getCustomerId());
    }

    private void verifyProductIdIsLinkedToOrderNo(PurchaseRequestDto request,
                                                  List<ProductEntity> productEntities) {
        var orderProductIdList =
                productEntities.stream().map(ProductEntity::getProductId)
                        .collect(Collectors.toList());
        for (var deliveryProduct : request.getDeliveryProducts()) {
            if (!orderProductIdList.contains(deliveryProduct.getProductId())) {
                throw new ProductNotLinkedToOrder(deliveryProduct.getProductId(),
                        request.getOrderNo());
            }
        }
    }

    private BigDecimal getDeliveryAmount(PurchaseRequestDto request,
                                         List<ProductEntity> productEntities) {
        var deliveryAmount = BigDecimal.ZERO;
        for (var productEntity : productEntities) {
            for (var deliveryProduct : request.getDeliveryProducts()) {
                if (productEntity.getProductId().equals(deliveryProduct.getProductId())) {
                    deliveryAmount = deliveryAmount.add(productEntity.getProductAmount());
                    productEntity.setDeliveryStatus(true);
                } else {
                    productEntity.setDeliveryStatus(false);
                }
            }
        }
        log.info("Delivery amount is : {}", deliveryAmount);
        return deliveryAmount;
    }

    private void completePrePurchaseOrder(OrderEntity order,
                                          CompletePrePurchaseRequest completePrePurchaseRequest) {
        AtlasClientException exception = null;
        try {
            var purchaseCompleteResponse =
                    atlasClient.completePrePurchase(completePrePurchaseRequest);
            var transactionId = purchaseCompleteResponse.getId();
            order.setTransactionId(transactionId);
            order.setTransactionStatus(TransactionStatus.COMPLETE_PRE_PURCHASE);
        } catch (AtlasClientException ex) {
            exception = ex;
            order.setTransactionStatus(TransactionStatus.FAIL_IN_COMPLETE_PRE_PURCHASE);
            log.error("Atlas complete pre purchase process was failed. orderNo - {}",
                    order.getOrderNo());
        }
        order.setRrn(completePrePurchaseRequest.getRrn());
        order.setTransactionDate(LocalDateTime.now());
        orderRepository.save(order);
        if (exception != null) {
            throw exception;
        }
    }

    public BigDecimal prePurchaseOrders(OperationEntity operationEntity, String cardId) {
        BigDecimal lastTempAmount = BigDecimal.ZERO;
        var orders = operationEntity.getOrders();
        validateOrdersForPrePurchase(orders, operationEntity.getId());
        for (var orderEntity : orders) {
            lastTempAmount = prePurchaseOrder(cardId, lastTempAmount, orderEntity);
        }
        if (lastTempAmount.compareTo(BigDecimal.ZERO) != 0) {
            var customerEntity = operationEntity.getCustomer();
            customerEntity.setLastTempAmount(
                    customerEntity.getLastTempAmount().add(lastTempAmount));
        }
        log.info(" Orders pre purchase process was finished...");
        return lastTempAmount;
    }

    private BigDecimal prePurchaseOrder(String cardId, BigDecimal lastTempAmount,
                                        OrderEntity orderEntity) {
        var totalOrderAmount = orderEntity.getTotalAmount().add(orderEntity.getCommission());
        var rrn = GenerateUtil.rrn();
        var prePurchaseRequest = PrePurchaseRequest.builder()
                .rrn(rrn).amount(totalOrderAmount).uid(cardId)
                .description(PRE_PURCHASE_DESCRIPTION + orderEntity.getOrderNo()).build();
        try {
            var purchaseResponse = atlasClient.prePurchase(prePurchaseRequest);
            orderEntity.setTransactionId(purchaseResponse.getId());
            orderEntity.setApprovalCode(purchaseResponse.getApprovalCode());
            orderEntity.setTransactionStatus(TransactionStatus.PRE_PURCHASE);
        } catch (AtlasClientException e) {
            lastTempAmount = lastTempAmount.add(totalOrderAmount);
            orderEntity.setTransactionStatus(TransactionStatus.FAIL_IN_PRE_PURCHASE);
        }
        orderEntity.setRrn(rrn);
        orderEntity.setTransactionDate(LocalDateTime.now());
        return lastTempAmount;
    }

    private void validateOrdersForPrePurchase(List<OrderEntity> orders, UUID trackId) {
        var isPrePurchasable = orders.stream()
                .allMatch(order -> order.getTransactionStatus() == null
                        || order.getTransactionStatus()
                        == TransactionStatus.FAIL_IN_PRE_PURCHASE);
        if (!isPrePurchasable) {
            log.error("No Permission for pre purchase. trackId - {}", trackId);
            throw new NoPermissionForTransaction("trackId - " + trackId);
        }
    }

    private CompletePrePurchaseRequest getCompletePrePurchaseRequest(OrderEntity order,
                                                                     BigDecimal deliveryAmount) {
        var rrn = GenerateUtil.rrn();
        var cardId = order.getOperation().getCustomer().getCardId();
        var percent = order.getOperation().getLoanPercent();
        var commission = amountUtil.getCommissionByPercent(deliveryAmount, percent);
        var totalPayment = commission.add(deliveryAmount);
        return CompletePrePurchaseRequest.builder()
                .id(Long.valueOf(order.getTransactionId()))
                .uid(cardId)
                .amount(totalPayment)
                .approvalCode(order.getApprovalCode())
                .rrn(rrn)
                .fee(commission)
                .description(COMPLETE_PRE_PURCHASE_DESCRIPTION + order.getOrderNo())
                .installments(order.getOperation().getLoanTerm())
                .build();
    }

    private CompletePrePurchaseRequest getCompletePrePurchaseRequest(OrderEntity order,
                                                                     String cardId) {
        var rrn = GenerateUtil.rrn();
        var commission = order.getCommission();
        var totalPayment = commission.add(order.getTotalAmount());
        return CompletePrePurchaseRequest.builder()
                .id(Long.valueOf(order.getTransactionId()))
                .uid(cardId)
                .amount(totalPayment)
                .approvalCode(order.getApprovalCode())
                .rrn(rrn)
                .description(COMPLETE_PRE_PURCHASE_FOR_REFUND_DESCRIPTION + order.getOrderNo())
                .build();
    }

    @Transactional(dontRollbackOn = AtlasClientException.class)
    public void refund(RefundRequestDto request) {
        log.info("Refund process is started : request - {}", request);
        var orderNo = request.getOrderNo();
        var orderEntity = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new OrderNotFoundException(ORDER_NO_LOG + orderNo));
        var transactionStatus = orderEntity.getTransactionStatus();
        if (transactionStatus == TransactionStatus.PRE_PURCHASE
                || transactionStatus == TransactionStatus.FAIL_IN_COMPLETE_PRE_PURCHASE) {
            var customerEntity = orderEntity.getOperation().getCustomer();
            if (!customerEntity.getId().equals(request.getCustomerId())) {
                throw new OrderNotLinkedToCustomer(
                        ORDER_NO_LOG + orderNo + "," + CUSTOMER_ID_LOG + customerEntity.getId());
            }
            var purchaseCompleteRequest =
                    getCompletePrePurchaseRequest(orderEntity, customerEntity.getCardId());
            completePrePurchaseOrder(orderEntity, purchaseCompleteRequest);
            refundAmount(orderEntity);
            log.info("Refund process was finished : orderNo - {}, customerId - {}",
                    request.getOrderNo(), request.getCustomerId());
        } else {
            throw new NoPermissionForTransaction(
                    ORDER_NO_LOG + orderEntity.getId() + " transactionStatus - "
                            + transactionStatus);
        }
    }

    private void refundAmount(OrderEntity orderEntity) {
        AtlasClientException exception = null;
        var rrn = GenerateUtil.rrn();
        var amountWithCommission =
                orderEntity.getTotalAmount().add(orderEntity.getCommission());
        try {
            var refundResponse = atlasClient.refund(orderEntity.getTransactionId(),
                    new RefundRequest(amountWithCommission, rrn));
            orderEntity.setTransactionId(refundResponse.getId());
            orderEntity.setTransactionStatus(TransactionStatus.REFUND);
        } catch (AtlasClientException ex) {
            exception = ex;
            orderEntity.setTransactionStatus(TransactionStatus.FAIL_IN_REFUND);
            log.error("Atlas refund process was failed : orderNo - {}",
                    orderEntity.getOrderNo());
        }
        orderEntity.setRrn(rrn);
        orderEntity.setTransactionDate(LocalDateTime.now());
        orderRepository.save(orderEntity);
        if (exception != null) {
            throw exception;
        }
    }

    private void validateCustomerBalance(CreateOrderRequestDto request, String cardId) {
        var purchaseAmount = getPurchaseAmount(request);
        var availableBalance = getAvailableBalance(cardId);
        if (purchaseAmount.compareTo(availableBalance) > 0) {
            throw new NoEnoughBalanceException(availableBalance);
        }
    }

    private void validateTotalOrderAmount(CreateOrderRequestDto createOrderRequestDto) {
        log.info("Validate total order amount is started...");
        for (var order : createOrderRequestDto.getDeliveryInfo()) {
            var productsTotalAmount = createOrderRequestDto.getProducts().stream()
                    .filter(orderProductItem -> orderProductItem.getOrderNo()
                            .equals(order.getOrderNo())).map(
                            OrderProductItem::getProductAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (order.getTotalAmount().compareTo(productsTotalAmount) != 0) {
                throw new NoMatchOrderAmountByProductException(order.getTotalAmount(),
                        productsTotalAmount);
            }
        }
        var operationTotalAmount = createOrderRequestDto.getTotalAmount();
        var calculatedTotalOrderAmount = createOrderRequestDto.getDeliveryInfo().stream()
                .map(OrderProductDeliveryInfo::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (calculatedTotalOrderAmount.compareTo(operationTotalAmount) != 0) {
            throw new NoMatchLoanAmountByOrderException(operationTotalAmount,
                    calculatedTotalOrderAmount);
        }
    }

    private void validatePurchaseAmountLimit(CreateOrderRequestDto request) {
        var purchaseAmount = getPurchaseAmount(request);
        var minLimitDifference = purchaseAmount.compareTo(BigDecimal.valueOf(50));
        var maxLimitDifference = purchaseAmount.compareTo(BigDecimal.valueOf(20000));
        if (minLimitDifference < 0 || maxLimitDifference > 0) {
            throw new TotalAmountLimitException(purchaseAmount);
        }
    }

    private BigDecimal getPurchaseAmount(CreateOrderRequestDto request) {
        var totalAmount = request.getTotalAmount();
        var loanTerm = request.getLoanTerm();
        var allOrderCommission = request.getDeliveryInfo().stream()
                .map(order -> amountUtil.getCommission(order.getTotalAmount(), loanTerm))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalAmount.add(allOrderCommission);
    }

    private BigDecimal getAvailableBalance(String cardId) {
        var cardDetailResponse = atlasClient.findCardByUid(cardId, ResultType.ACCOUNT);
        var primaryAccount = cardDetailResponse.getAccounts().stream()
                .filter(x -> x.getStatus() == AccountStatus.OPEN_PRIMARY).findFirst();
        if (primaryAccount.isEmpty()) {
            log.warn("Account not found in open primary status.cardId - {}", cardId);
            return BigDecimal.ZERO;
        }
        return primaryAccount.get().getAvailableBalance();
    }
}
