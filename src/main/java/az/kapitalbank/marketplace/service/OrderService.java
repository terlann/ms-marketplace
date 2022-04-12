package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constant.AtlasConstant.REVERSE_DESCRIPTION;
import static az.kapitalbank.marketplace.constant.CommonConstant.CUSTOMER_ID_LOG;
import static az.kapitalbank.marketplace.constant.CommonConstant.ORDER_NO_LOG;
import static az.kapitalbank.marketplace.constant.CommonConstant.TELESALES_ORDER_ID_LOG;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.exception.AtlasClientException;
import az.kapitalbank.marketplace.client.atlas.model.request.PurchaseCompleteRequest;
import az.kapitalbank.marketplace.client.atlas.model.request.PurchaseRequest;
import az.kapitalbank.marketplace.client.atlas.model.request.ReversePurchaseRequest;
import az.kapitalbank.marketplace.constant.AccountStatus;
import az.kapitalbank.marketplace.constant.OrderStatus;
import az.kapitalbank.marketplace.constant.ResultType;
import az.kapitalbank.marketplace.constant.ScoringStatus;
import az.kapitalbank.marketplace.constant.TransactionStatus;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.dto.OrderProductDeliveryInfo;
import az.kapitalbank.marketplace.dto.OrderProductItem;
import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.dto.request.PurchaseRequestDto;
import az.kapitalbank.marketplace.dto.request.ReverseRequestDto;
import az.kapitalbank.marketplace.dto.request.TelesalesResultRequestDto;
import az.kapitalbank.marketplace.dto.response.CheckOrderResponseDto;
import az.kapitalbank.marketplace.dto.response.CreateOrderResponse;
import az.kapitalbank.marketplace.dto.response.PurchaseResponseDto;
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
import az.kapitalbank.marketplace.messaging.sender.FraudCheckSender;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import az.kapitalbank.marketplace.repository.OperationRepository;
import az.kapitalbank.marketplace.repository.OrderRepository;
import az.kapitalbank.marketplace.util.AmountUtil;
import az.kapitalbank.marketplace.util.GenerateUtil;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    FraudCheckSender customerOrderProducer;
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
        Optional<String> sendDecision;
        if (request.getScoringStatus() == ScoringStatus.APPROVED) {
            sendDecision = telesalesResultApproveProcess(request, operationEntity);
        } else {
            operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.REJECTED);
            operationEntity.setScoringStatus(ScoringStatus.REJECTED);
            sendDecision = umicoService.sendRejectedDecision(operationEntity.getId());
        }
        sendDecision.ifPresent(operationEntity::setUmicoDecisionError);
        operationRepository.save(operationEntity);
    }

    private Optional<String> telesalesResultApproveProcess(TelesalesResultRequestDto request,
                                                           OperationEntity operationEntity) {
        String cardId;
        if (request.getUid() != null) {
            cardId = request.getUid();
        } else {
            cardId = atlasClient.findByPan(request.getPan()).getUid();
        }
        prePurchaseOrders(operationEntity, cardId);
        operationEntity.setUmicoDecisionStatus(UmicoDecisionStatus.APPROVED);
        operationEntity.setScoringStatus(ScoringStatus.APPROVED);
        operationEntity.setScoringDate(LocalDateTime.now());
        operationEntity.setLoanContractStartDate(request.getLoanContractStartDate());
        operationEntity.setLoanContractEndDate(request.getLoanContractEndDate());
        operationEntity.setScoredAmount(request.getScoredAmount());
        var customerEntity = operationEntity.getCustomer();
        customerEntity.setCardId(cardId);
        customerEntity.setCompleteProcessDate(LocalDateTime.now());
        return umicoService.sendApprovedDecision(operationEntity, customerEntity.getId());
    }

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequestDto request) {
        log.info("Create order process is started : request - {}", request);
        validateTotalOrderAmount(request);
        var customerId = request.getCustomerInfo().getCustomerId();
        CustomerEntity customerEntity = getCustomerEntity(request, customerId);
        OperationEntity operationEntity = saveOperationEntity(request, customerEntity);
        var trackId = operationEntity.getId();
        if (customerId == null && customerEntity.getCompleteProcessDate() == null) {
            log.info("First transaction process is started : customerId - {}, trackId - {}",
                    customerEntity.getId(), trackId);
            var fraudCheckEvent = orderMapper.toOrderEvent(request);
            fraudCheckEvent.setTrackId(trackId);
            customerOrderProducer.sendMessage(fraudCheckEvent);
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
            } else {
                customerEntity = customerMapper.toCustomerEntity(request.getCustomerInfo());
                customerEntity = customerRepository.save(customerEntity);
                log.info("New customer was created. customerId" + customerEntity.getId());
            }
        } else {
            customerEntity = customerRepository.findById(customerId)
                    .orElseThrow(() -> new CustomerNotFoundException(CUSTOMER_ID_LOG + customerId));
            var isExistsCustomerByDecisionStatus =
                    operationRepository.existsByCustomerAndUmicoDecisionStatusIn(customerEntity,
                            Set.of(UmicoDecisionStatus.PENDING, UmicoDecisionStatus.PREAPPROVED));
            if (isExistsCustomerByDecisionStatus) {
                throw new CustomerNotCompletedProcessException(CUSTOMER_ID_LOG + customerId);
            }
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
        log.info("Purchase process is started : request - {}", request);
        var orderEntity = orderRepository.findByOrderNo(request.getOrderNo())
                .orElseThrow(() -> new OrderNotFoundException("orderNo - " + request.getOrderNo()));
        var transactionStatus = orderEntity.getTransactionStatus();
        var productEntities = orderEntity.getProducts();
        verifyProductIdIsLinkedToOrderNo(request, productEntities);
        if (transactionStatus == TransactionStatus.PURCHASE
                || transactionStatus == TransactionStatus.FAIL_IN_REVERSE
                || transactionStatus == TransactionStatus.FAIL_IN_COMPLETE) {
            var deliveryAmount = getDeliveryAmount(request, productEntities);
            if (deliveryAmount.compareTo(BigDecimal.ZERO) > 0) {
                purchaseOrder(orderEntity, deliveryAmount);
            }
        } else {
            log.error("No Permission for complete. orderNo - {}, transactionStatus -{}",
                    orderEntity.getOrderNo(), orderEntity.getTransactionStatus());
            throw new NoPermissionForTransaction(
                    ORDER_NO_LOG + orderEntity.getId() + " transactionStatus - "
                            + transactionStatus);
        }
        log.info("Purchase process was finished : trackId - {}, customerId - {}",
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

    private void purchaseOrder(OrderEntity order, BigDecimal deliveryAmount) {
        AtlasClientException exception = null;
        var cardId = order.getOperation().getCustomer().getCardId();
        var rrn = GenerateUtil.rrn();
        var purchaseCompleteRequest =
                getPurchaseCompleteRequest(cardId, order, rrn, deliveryAmount);
        try {
            var purchaseCompleteResponse = atlasClient.complete(purchaseCompleteRequest);
            var transactionId = purchaseCompleteResponse.getId();
            order.setTransactionId(transactionId);
            order.setTransactionStatus(TransactionStatus.COMPLETE);
        } catch (AtlasClientException ex) {
            exception = ex;
            order.setTransactionStatus(TransactionStatus.FAIL_IN_COMPLETE);
            order.setTransactionError(ex.getMessage());
            log.error("Atlas complete process was failed. orderNo - {}", order.getOrderNo());
        }
        order.setRrn(rrn);
        order.setTransactionDate(LocalDateTime.now());
        orderRepository.save(order);
        if (exception != null) {
            throw exception;
        }
    }

    public void prePurchaseOrders(OperationEntity operationEntity, String cardId) {
        for (var orderEntity : operationEntity.getOrders()) {
            var rrn = GenerateUtil.rrn();
            var purchaseRequest = PurchaseRequest.builder().rrn(rrn)
                    .amount(orderEntity.getTotalAmount().add(orderEntity.getCommission()))
                    .description("fee=" + orderEntity.getCommission()).uid(cardId).build();
            try {
                var purchaseResponse = atlasClient.purchase(purchaseRequest);
                orderEntity.setTransactionId(purchaseResponse.getId());
                orderEntity.setApprovalCode(purchaseResponse.getApprovalCode());
                orderEntity.setTransactionStatus(TransactionStatus.PURCHASE);
            } catch (AtlasClientException e) {
                orderEntity.setTransactionStatus(TransactionStatus.FAIL_IN_PURCHASE);
                orderEntity.setTransactionError(e.getMessage());
            }
            orderEntity.setRrn(rrn);
            orderEntity.setTransactionDate(LocalDateTime.now());
        }
        log.info(" Orders purchase process was finished...");
    }

    private PurchaseCompleteRequest getPurchaseCompleteRequest(String cardId, OrderEntity
            order, String rrn, BigDecimal deliveryAmount) {
        var percent = order.getOperation().getLoanPercent();
        var commission = amountUtil.getCommissionByPercent(deliveryAmount, percent);
        var totalPayment = commission.add(deliveryAmount);
        return PurchaseCompleteRequest.builder()
                .id(Long.valueOf(order.getTransactionId()))
                .uid(cardId)
                .amount(totalPayment)
                .approvalCode(order.getApprovalCode())
                .rrn(rrn)
                .installments(order.getOperation().getLoanTerm())
                .build();

    }

    @Transactional
    public PurchaseResponseDto reverse(ReverseRequestDto request) {
        log.info("Reverse process is started : request - {}", request);
        var customerId = request.getCustomerId();
        var orderNo = request.getOrderNo();
        var orderEntity = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new OrderNotFoundException(ORDER_NO_LOG + orderNo));
        var transactionStatus = orderEntity.getTransactionStatus();
        if (transactionStatus == TransactionStatus.PURCHASE
                || transactionStatus == TransactionStatus.FAIL_IN_REVERSE
                || transactionStatus == TransactionStatus.FAIL_IN_COMPLETE) {
            var customerEntity = orderEntity.getOperation().getCustomer();
            if (!customerEntity.getId().equals(request.getCustomerId())) {
                throw new OrderNotLinkedToCustomer(
                        ORDER_NO_LOG + orderNo + "," + CUSTOMER_ID_LOG + customerId);
            }
            var purchaseResponse = reverseOrder(orderEntity);
            log.info("Reverse process was finished : orderNo - {}, customerId - {}",
                    request.getOrderNo(), request.getCustomerId());
            return purchaseResponse;
        }
        throw new NoPermissionForTransaction(
                ORDER_NO_LOG + orderEntity.getId() + " transactionStatus - "
                        + transactionStatus);
    }

    private PurchaseResponseDto reverseOrder(OrderEntity orderEntity) {
        var purchaseResponse = new PurchaseResponseDto();
        try {
            var reverseResponse = atlasClient.reverse(orderEntity.getTransactionId(),
                    new ReversePurchaseRequest(REVERSE_DESCRIPTION));
            orderEntity.setTransactionId(String.valueOf(reverseResponse.getId()));
            orderEntity.setTransactionStatus(TransactionStatus.REVERSE);
            purchaseResponse.setStatus(OrderStatus.SUCCESS);
        } catch (AtlasClientException ex) {
            orderEntity.setTransactionStatus(TransactionStatus.FAIL_IN_REVERSE);
            orderEntity.setTransactionError(ex.getMessage());
            purchaseResponse.setStatus(OrderStatus.FAIL);
            log.error("Atlas reverse process was failed : orderNo - {}",
                    orderEntity.getOrderNo());
            String errorMessager = "Atlas Exception: UUID - %s  ,  code - %s, message - %s";
            log.error(
                    String.format(errorMessager, ex.getUuid(), ex.getCode(), ex.getMessage()));
        }
        orderEntity.setTransactionDate(LocalDateTime.now());
        orderRepository.save(orderEntity);
        purchaseResponse.setOrderNo(orderEntity.getOrderNo());
        return purchaseResponse;
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
