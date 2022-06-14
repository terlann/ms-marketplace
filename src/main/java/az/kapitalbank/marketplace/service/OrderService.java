package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constant.AtlasConstant.COMPLETE_PRE_PURCHASE_DESCRIPTION;
import static az.kapitalbank.marketplace.constant.AtlasConstant.COMPLETE_PRE_PURCHASE_FOR_REFUND_DESCRIPTION;
import static az.kapitalbank.marketplace.constant.AtlasConstant.PRE_PURCHASE_DESCRIPTION;
import static az.kapitalbank.marketplace.constant.CommonConstant.CUSTOMER_ID_LOG;
import static az.kapitalbank.marketplace.constant.CommonConstant.CUSTOMER_NOT_FOUND_LOG;
import static az.kapitalbank.marketplace.constant.CommonConstant.ORDER_NO_EXCEPTION_LOG;
import static az.kapitalbank.marketplace.constant.CommonConstant.ORDER_NO_LOG;
import static az.kapitalbank.marketplace.constant.CommonConstant.ORDER_NO_REQUEST_LOG;
import static az.kapitalbank.marketplace.constant.CommonConstant.ORDER_NO_RESPONSE_LOG;
import static az.kapitalbank.marketplace.constant.CommonConstant.TELESALES_ORDER_ID_LOG;
import static az.kapitalbank.marketplace.constant.TransactionStatus.COMPLETE_PRE_PURCHASE;
import static az.kapitalbank.marketplace.constant.TransactionStatus.FAIL_IN_COMPLETE_PRE_PURCHASE;
import static az.kapitalbank.marketplace.constant.TransactionStatus.FAIL_IN_COMPLETE_REFUND;
import static az.kapitalbank.marketplace.constant.TransactionStatus.FAIL_IN_PRE_PURCHASE;
import static az.kapitalbank.marketplace.constant.TransactionStatus.FAIL_IN_REFUND;
import static az.kapitalbank.marketplace.constant.TransactionStatus.PRE_PURCHASE;
import static az.kapitalbank.marketplace.constant.TransactionStatus.REFUND;
import static az.kapitalbank.marketplace.constant.UmicoDecisionStatus.APPROVED;
import static az.kapitalbank.marketplace.constant.UmicoDecisionStatus.FAIL_IN_PENDING;
import static az.kapitalbank.marketplace.constant.UmicoDecisionStatus.FAIL_IN_PREAPPROVED;
import static az.kapitalbank.marketplace.constant.UmicoDecisionStatus.PENDING;
import static az.kapitalbank.marketplace.constant.UmicoDecisionStatus.PREAPPROVED;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.model.request.CompletePrePurchaseRequest;
import az.kapitalbank.marketplace.client.atlas.model.request.PrePurchaseRequest;
import az.kapitalbank.marketplace.client.atlas.model.request.RefundRequest;
import az.kapitalbank.marketplace.constant.AccountStatus;
import az.kapitalbank.marketplace.constant.Error;
import az.kapitalbank.marketplace.constant.OperationRejectReason;
import az.kapitalbank.marketplace.constant.ResultType;
import az.kapitalbank.marketplace.constant.ScoringStatus;
import az.kapitalbank.marketplace.dto.OrderProductDeliveryInfo;
import az.kapitalbank.marketplace.dto.OrderProductItem;
import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.dto.request.DeliveryRequestDto;
import az.kapitalbank.marketplace.dto.request.PaybackRequestDto;
import az.kapitalbank.marketplace.dto.request.TelesalesResultRequestDto;
import az.kapitalbank.marketplace.dto.response.CheckOrderResponseDto;
import az.kapitalbank.marketplace.dto.response.CreateOrderResponse;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.entity.ProductEntity;
import az.kapitalbank.marketplace.exception.CommonException;
import az.kapitalbank.marketplace.exception.DeliveryException;
import az.kapitalbank.marketplace.exception.PaybackException;
import az.kapitalbank.marketplace.mapper.CustomerMapper;
import az.kapitalbank.marketplace.mapper.OperationMapper;
import az.kapitalbank.marketplace.mapper.OrderMapper;
import az.kapitalbank.marketplace.messaging.publisher.FraudCheckPublisher;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import az.kapitalbank.marketplace.repository.OperationRepository;
import az.kapitalbank.marketplace.repository.OrderRepository;
import az.kapitalbank.marketplace.util.CommissionUtil;
import az.kapitalbank.marketplace.util.RrnUtil;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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

    CommissionUtil commissionUtil;
    SmsService smsService;
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
                .orElseThrow(() -> new CommonException(Error.OPERATION_NOT_FOUND,
                        "Operation not found." + TELESALES_ORDER_ID_LOG + telesalesOrderId));
        var trackId = operationEntity.getId();
        if (operationEntity.getScoringStatus() != null) {
            throw new CommonException(Error.OPERATION_ALREADY_SCORED,
                    "Operation had already scored." + TELESALES_ORDER_ID_LOG + telesalesOrderId);
        }
        operationEntity.setScoringDate(LocalDateTime.now());
        if (request.getScoringStatus() == ScoringStatus.REJECTED) {
            var umicoDecisionStatus = umicoService.sendRejectedDecision(operationEntity.getId());
            operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
            operationEntity.setRejectReason(OperationRejectReason.TELESALES);
            operationEntity.setScoringStatus(ScoringStatus.REJECTED);
            log.info("Telesales result : Customer was failed telesales process : "
                    + "trackId - {} , scoringStatus - {}", trackId, request.getScoringStatus());
            return;
        }
        telesalesResultApproveProcess(request, operationEntity);
    }

    private void telesalesResultApproveProcess(TelesalesResultRequestDto request,
                                               OperationEntity operationEntity) {
        operationEntity.setUmicoDecisionStatus(APPROVED);
        operationEntity.setScoringStatus(ScoringStatus.APPROVED);
        operationEntity.setLoanContractStartDate(request.getLoanContractStartDate());
        operationEntity.setLoanContractEndDate(request.getLoanContractEndDate());
        operationEntity.setScoredAmount(request.getScoredAmount());
        var customerEntity = operationEntity.getCustomer();
        var cardId = request.getUid();
        customerEntity.setCardId(cardId);
        var trackId = operationEntity.getId();
        var lastTempAmount = prePurchaseOrders(operationEntity, cardId);
        if (lastTempAmount.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Telesales result : Pre purchase was finished : trackId - {}", trackId);
            smsService.sendCompleteScoringSms(operationEntity);
            smsService.sendPrePurchaseSms(operationEntity);
        } else {
            log.info("Telesales result : Pre purchase was failed : trackId - {}", trackId);
        }
        var customerId = customerEntity.getId();
        var umicoDecisionStatus =
                umicoService.sendApprovedDecision(operationEntity, customerId);
        operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
        log.info("Telesales result : Customer was finished telesales process : "
                + "trackId - {} , customerId - {}", trackId, customerId);
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
            var fraudCheckEvent = orderMapper.toFraudCheckEvent(request);
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
        operationEntity.setIsOtpOperation(request.getCustomerInfo().getCustomerId() != null
                && customerEntity.getCardId() != null);
        saveOrderEntities(request, operationEntity);
        operationEntity.setLoanPercent(commissionUtil.getCommissionPercent(request.getLoanTerm()));
        operationEntity = operationRepository.save(operationEntity);
        return operationEntity;
    }

    private void saveOrderEntities(CreateOrderRequestDto request, OperationEntity operationEntity) {
        var operationCommission = BigDecimal.ZERO;
        var orderEntities = new ArrayList<OrderEntity>();
        for (OrderProductDeliveryInfo deliveryInfo : request.getDeliveryInfo()) {
            var productEntities = new ArrayList<ProductEntity>();
            var commission =
                    commissionUtil.getCommission(deliveryInfo.getTotalAmount(),
                            request.getLoanTerm());
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
            orderEntity.setProducts(productEntities);
            orderEntities.add(orderEntity);
        }
        operationEntity.setCommission(operationCommission);
        operationEntity.setOrders(orderEntities);
    }

    private CustomerEntity getCustomerEntity(CreateOrderRequestDto request, UUID customerId) {
        CustomerEntity customerEntity;
        if (customerId == null) {
            log.info("First order create process is started...");
            var umicoUserId = request.getCustomerInfo().getUmicoUserId();
            checkAdditionalPhoneNumbersEquals(request);
            validatePurchaseAmountLimit(request);
            var customerByUmicoUserId = customerRepository
                    .findByUmicoUserId(umicoUserId);
            if (customerByUmicoUserId.isPresent()) {
                if (customerByUmicoUserId.get().getCardId() != null) {
                    throw new CommonException(Error.CUSTOMER_ID_SKIPPED,
                            "Skipped customer id in request : umicoUserId -" + umicoUserId);
                }
                customerEntity = customerByUmicoUserId.get();
                checkCustomerIncompleteProcess(customerEntity);
            } else {
                customerEntity = customerMapper.toCustomerEntity(request.getCustomerInfo());
                customerEntity = customerRepository.save(customerEntity);
                log.info("New customer was created : customerId - {}", customerEntity.getId());
            }
        } else {
            customerEntity = customerRepository.findById(customerId)
                    .orElseThrow(() -> new CommonException(Error.CUSTOMER_NOT_FOUND,
                            CUSTOMER_NOT_FOUND_LOG + CUSTOMER_ID_LOG + customerId));
            validateCustomerBalance(request, customerEntity.getCardId());
        }
        return customerEntity;
    }

    private void checkCustomerIncompleteProcess(CustomerEntity customerEntity) {
        var decisions = Stream.of(PENDING, FAIL_IN_PENDING, PREAPPROVED, FAIL_IN_PREAPPROVED)
                .map(Enum::name)
                .collect(Collectors.toList());
        var isExistsCustomerByDecisionStatus = operationRepository
                .existsByCustomerIdAndUmicoDecisionStatuses(customerEntity.getId().toString(),
                        decisions);
        if (isExistsCustomerByDecisionStatus) {
            throw new CommonException(Error.CUSTOMER_NOT_COMPLETED_PROCESS,
                    "Customer has not yet completed the process : " + CUSTOMER_ID_LOG
                            + customerEntity.getId());
        }
    }

    private void checkAdditionalPhoneNumbersEquals(CreateOrderRequestDto request) {
        var firstPhoneNumber = request.getCustomerInfo().getAdditionalPhoneNumber1();
        var secondPhoneNumber = request.getCustomerInfo().getAdditionalPhoneNumber2();
        if (firstPhoneNumber.equals(secondPhoneNumber)) {
            throw new CommonException(Error.UNIQUE_PHONE_NUMBER, String.format(
                    "Additional numbers aren't different : additionalPhoneNumber1 = %s ,"
                            + " additionalPhoneNumber2 = %s", firstPhoneNumber, secondPhoneNumber));
        }
    }

    public CheckOrderResponseDto checkOrder(String telesalesOrderId) {
        log.info("Check order is started : telesalesOrderId  - {}", telesalesOrderId);
        var operationEntityOptional = operationRepository.findByTelesalesOrderId(telesalesOrderId);
        var operationEntity = operationEntityOptional.orElseThrow(
                () -> new CommonException(Error.OPERATION_NOT_FOUND,
                        "Operation not found." + TELESALES_ORDER_ID_LOG + telesalesOrderId));
        var scoringStatus = operationEntity.getScoringStatus();
        if (scoringStatus != null) {
            throw new CommonException(Error.OPERATION_ALREADY_SCORED,
                    "Operation had already scored. TelesalesOrderId - " + telesalesOrderId);
        }
        var orderResponseDto = orderMapper.toCheckOrderResponseDto(operationEntity);
        log.info("Check order was finished : telesalesOrderId - {}", telesalesOrderId);
        return orderResponseDto;
    }

    @Transactional(dontRollbackOn = DeliveryException.class)
    public void delivery(DeliveryRequestDto request) {
        log.info("Delivery process is started : request - {}", request);
        var order = orderRepository.findByOrderNo(request.getOrderNo())
                .orElseThrow(() -> new CommonException(Error.ORDER_NOT_FOUND,
                        "Order not found. orderNo - " + request.getOrderNo()));

        var transactionStatus = order.getTransactionStatus();
        if (transactionStatus != PRE_PURCHASE) {
            log.error("No Permission for complete pre purchase in delivery : "
                            + "orderNo - {}, transactionStatus - {}",
                    order.getOrderNo(), order.getTransactionStatus());
            throw new CommonException(Error.NO_PERMISSION, ORDER_NO_LOG + order.getOrderNo());
        }

        var productEntities = order.getProducts();
        verifyProductIdIsLinkedToOrderNo(request, productEntities);

        var customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(
                        () -> new CommonException(Error.CUSTOMER_NOT_FOUND, "Customer not found : "
                                + CUSTOMER_ID_LOG + request.getCustomerId()));

        var deliveredOrderAmount = getDeliveredOrderAmount(request, productEntities);
        if (deliveredOrderAmount.compareTo(BigDecimal.ZERO) == 0) {
            throw new CommonException(Error.NO_DELIVERY_PRODUCTS,
                    "No delivery products for this order." + ORDER_NO_LOG + order.getOrderNo());
        }

        completePrePurchaseOrder(order, deliveredOrderAmount, customer.getCardId());

        if (order.getTransactionStatus() == FAIL_IN_COMPLETE_PRE_PURCHASE) {
            throw new DeliveryException(ORDER_NO_LOG + order.getOrderNo());
        }
        log.info("Delivery process was finished : orderNo - {}, customerId - {}",
                request.getOrderNo(), request.getCustomerId());
    }

    private void verifyProductIdIsLinkedToOrderNo(DeliveryRequestDto request,
                                                  List<ProductEntity> productEntities) {
        var orderProductIdList =
                productEntities.stream().map(ProductEntity::getProductNo)
                        .collect(Collectors.toList());
        for (var deliveryProduct : request.getDeliveryProducts()) {
            if (!orderProductIdList.contains(deliveryProduct.getProductId())) {
                throw new CommonException(Error.PRODUCT_NOT_LINKED_TO_ORDER,
                        String.format(
                                "Product is not linked to order : productId - %s, orderNo - %s",
                                deliveryProduct.getProductId(), request.getOrderNo()));
            }
        }
    }

    private BigDecimal getDeliveredOrderAmount(DeliveryRequestDto request,
                                               List<ProductEntity> productEntities) {
        var deliveredOrderAmount = BigDecimal.ZERO;
        for (var productEntity : productEntities) {
            for (var deliveryProduct : request.getDeliveryProducts()) {
                if (productEntity.getProductNo().equals(deliveryProduct.getProductId())) {
                    deliveredOrderAmount =
                            deliveredOrderAmount.add(productEntity.getAmount());
                    productEntity.setDeliveryStatus(true);
                    break;
                }
            }
            if (productEntity.getDeliveryStatus() == null) {
                productEntity.setDeliveryStatus(false);
            }
        }
        log.info("Delivered order amount is : {}, orderNo - {}", deliveredOrderAmount,
                request.getOrderNo());
        return deliveredOrderAmount;
    }

    private void completePrePurchaseOrder(OrderEntity order,
                                          BigDecimal deliveredOrderAmount,
                                          String cardId) {
        var orderNo = order.getOrderNo();
        var percent = order.getOperation().getLoanPercent();
        var commission = commissionUtil.getCommissionByPercent(deliveredOrderAmount, percent);
        var totalPayment = commission.add(deliveredOrderAmount);
        var rrn = RrnUtil.rrn();
        var completePrePurchaseRequest = CompletePrePurchaseRequest.builder()
                .id(Long.valueOf(order.getTransactionId())).uid(cardId).amount(totalPayment)
                .approvalCode(order.getApprovalCode()).rrn(rrn)
                .fee(commission).description(COMPLETE_PRE_PURCHASE_DESCRIPTION + order.getOrderNo())
                .installments(order.getOperation().getLoanTerm())
                .build();
        order.setRrn(rrn);
        try {
            log.info("Atlas complete pre purchase is started : "
                    + ORDER_NO_REQUEST_LOG, orderNo, completePrePurchaseRequest);
            var completePrePurchaseResponse =
                    atlasClient.completePrePurchase(completePrePurchaseRequest);
            order.setTransactionId(completePrePurchaseResponse.getId());
            order.setTransactionStatus(COMPLETE_PRE_PURCHASE);
            order.setTransactionDate(LocalDateTime.now());
            log.info("Atlas complete pre purchase was finished : " + ORDER_NO_RESPONSE_LOG,
                    orderNo, completePrePurchaseResponse);
        } catch (FeignException e) {
            order.setTransactionStatus(FAIL_IN_COMPLETE_PRE_PURCHASE);
            log.error("Atlas complete pre purchase was failed : " + ORDER_NO_EXCEPTION_LOG,
                    orderNo, e);
        }
    }

    private void completePrePurchaseOrder(OrderEntity order,
                                          String cardId) {
        var orderNo = order.getOrderNo();
        var commission = order.getCommission();
        var totalPayment = commission.add(order.getTotalAmount());
        var rrn = RrnUtil.rrn();
        var completePrePurchaseRequest = CompletePrePurchaseRequest.builder()
                .id(Long.valueOf(order.getTransactionId()))
                .uid(cardId)
                .amount(totalPayment)
                .approvalCode(order.getApprovalCode())
                .rrn(rrn)
                .description(COMPLETE_PRE_PURCHASE_FOR_REFUND_DESCRIPTION + order.getOrderNo())
                .build();
        order.setRrn(rrn);
        try {
            log.info("Atlas complete pre purchase for refund  is started : " + ORDER_NO_REQUEST_LOG,
                    orderNo, completePrePurchaseRequest);
            var completePrePurchaseResponse =
                    atlasClient.completePrePurchase(completePrePurchaseRequest);
            order.setTransactionId(completePrePurchaseResponse.getId());
            order.setTransactionDate(LocalDateTime.now());
            log.info("Atlas complete pre purchase for refund  was finished : "
                    + ORDER_NO_RESPONSE_LOG, orderNo, completePrePurchaseResponse);
        } catch (FeignException e) {
            order.setTransactionStatus(FAIL_IN_COMPLETE_REFUND);
            log.error(
                    "Atlas complete pre purchase for refund was failed : " + ORDER_NO_EXCEPTION_LOG,
                    orderNo, e);
        }
    }

    public BigDecimal prePurchaseOrders(OperationEntity operationEntity, String cardId) {
        BigDecimal lastTempAmount = BigDecimal.ZERO;
        var orders = operationEntity.getOrders();
        validateOrdersForPrePurchase(orders, operationEntity.getId());
        for (var orderEntity : orders) {
            lastTempAmount = lastTempAmount.add(prePurchaseOrder(cardId, orderEntity));
        }
        if (lastTempAmount.compareTo(BigDecimal.ZERO) != 0) {
            var customerEntity = operationEntity.getCustomer();
            customerEntity.setLastTempAmount(
                    customerEntity.getLastTempAmount().add(lastTempAmount));
        }
        log.info("Orders pre purchase process was finished : trackId - {}, lastTempAmount - {}",
                operationEntity.getId(), lastTempAmount);
        return lastTempAmount;
    }

    private BigDecimal prePurchaseOrder(String cardId, OrderEntity order) {
        var orderNo = order.getOrderNo();
        var totalOrderAmount = order.getTotalAmount().add(order.getCommission());
        var rrn = RrnUtil.rrn();
        var prePurchaseRequest = PrePurchaseRequest.builder()
                .rrn(rrn).amount(totalOrderAmount).uid(cardId)
                .description(PRE_PURCHASE_DESCRIPTION + order.getOrderNo()).build();
        order.setRrn(rrn);
        try {
            log.info("Atlas pre purchase is started : "
                    + ORDER_NO_REQUEST_LOG, orderNo, prePurchaseRequest);
            var prePurchaseResponse = atlasClient.prePurchase(prePurchaseRequest);
            order.setTransactionId(prePurchaseResponse.getId());
            order.setApprovalCode(prePurchaseResponse.getApprovalCode());
            order.setTransactionStatus(PRE_PURCHASE);
            order.setTransactionDate(LocalDateTime.now());
            log.info("Atlas pre purchase was finished : " + ORDER_NO_RESPONSE_LOG,
                    orderNo, prePurchaseResponse);
            return BigDecimal.ZERO;
        } catch (FeignException e) {
            order.setTransactionStatus(FAIL_IN_PRE_PURCHASE);
            log.error("Atlas pre purchase was failed : " + ORDER_NO_EXCEPTION_LOG, orderNo, e);
            return order.getTotalAmount().add(order.getCommission());

        }
    }

    private void validateOrdersForPrePurchase(List<OrderEntity> orders, UUID trackId) {
        var isPrePurchasable = orders.stream()
                .allMatch(order -> order.getTransactionStatus() == null);
        if (!isPrePurchasable) {
            log.error("No Permission for pre purchase : trackId - {}", trackId);
            throw new CommonException(Error.NO_PERMISSION, "trackId - " + trackId);
        }
    }

    @Transactional
    public void autoPayback() {
        var transactionDate = LocalDateTime.now().minusDays(21);
        var orders =
                orderRepository.findByTransactionDateBeforeAndTransactionStatus(transactionDate,
                        PRE_PURCHASE);
        orders.forEach(order -> {
            var orderNo = order.getOrderNo();
            log.info("Auto payback process is started : orderNo - {}", orderNo);
            var cardId = order.getOperation().getCustomer().getCardId();
            completePrePurchaseOrder(order, cardId);
            if (order.getTransactionStatus() == FAIL_IN_COMPLETE_REFUND) {
                order.setIsAutoPayback(false);
                log.error("Auto payback was failed complete for refund : orderNo - {}",
                        orderNo);
                return;
            }

            refundOrder(order);
            if (order.getTransactionStatus() == FAIL_IN_REFUND) {
                order.setIsAutoPayback(false);
                log.error("Auto payback was failed for refund : orderNo - {}", orderNo);
                return;
            }
            order.setIsAutoPayback(true);
            log.info("Auto payback process was finished : orderNo - {}", orderNo);
        });
    }

    @Transactional(dontRollbackOn = PaybackException.class)
    public void payback(PaybackRequestDto request) {
        log.info("Payback process is started : request - {}", request);
        var orderNo = request.getOrderNo();
        var order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new CommonException(Error.ORDER_NOT_FOUND,
                        "Order not found : orderNo - " + orderNo));

        var transactionStatus = order.getTransactionStatus();
        if (transactionStatus != PRE_PURCHASE) {
            log.error("No Permission for refund in payback : "
                            + "orderNo - {}, transactionStatus - {}",
                    order.getOrderNo(), order.getTransactionStatus());
            throw new CommonException(Error.NO_PERMISSION, ORDER_NO_LOG + orderNo);
        }

        var customer = customerRepository.findById(request.getCustomerId())
                .orElseThrow(
                        () -> new CommonException(Error.CUSTOMER_NOT_FOUND, CUSTOMER_NOT_FOUND_LOG
                                + CUSTOMER_ID_LOG + request.getCustomerId()));

        completePrePurchaseOrder(order, customer.getCardId());
        if (order.getTransactionStatus() == FAIL_IN_COMPLETE_REFUND) {
            throw new PaybackException(ORDER_NO_LOG + orderNo);
        }

        refundOrder(order);
        if (order.getTransactionStatus() == FAIL_IN_REFUND) {
            throw new PaybackException(ORDER_NO_LOG + orderNo);
        }
        log.info("Payback process was finished : orderNo - {}, customerId - {}",
                orderNo, request.getCustomerId());
    }

    private void refundOrder(OrderEntity order) {
        var orderNo = order.getOrderNo();
        var transactionId = order.getTransactionId();
        var amountWithCommission = order.getTotalAmount().add(order.getCommission());
        var rrn = RrnUtil.rrn();
        var refundRequest = new RefundRequest(amountWithCommission, rrn);
        order.setRrn(rrn);
        try {
            log.info("Atlas refund process is started : orderNo - {}, transactionId - {}, "
                    + "request - {}", orderNo, transactionId, refundRequest);
            var refundResponse = atlasClient.refund(transactionId, refundRequest);
            order.setTransactionId(refundResponse.getId());
            order.setTransactionStatus(REFUND);
            order.setTransactionDate(LocalDateTime.now());
            log.info("Atlas refund process was finished : " + ORDER_NO_RESPONSE_LOG,
                    orderNo, refundResponse);
        } catch (FeignException e) {
            order.setTransactionStatus(FAIL_IN_REFUND);
            log.error("Atlas pre purchase was failed : " + ORDER_NO_EXCEPTION_LOG, orderNo, e);
        }
    }

    private void validateCustomerBalance(CreateOrderRequestDto request, String cardId) {
        var purchaseAmount = getPurchaseAmount(request);
        var availableBalance = getAvailableBalance(cardId);
        if (purchaseAmount.compareTo(availableBalance) > 0) {
            throw new CommonException(Error.NO_ENOUGH_BALANCE,
                    "There is no enough amount in balance : availableBalance - "
                            + availableBalance);
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
                throw new CommonException(Error.NO_MATCH_ORDER_AMOUNT_BY_PRODUCTS, String.format(
                        "Order amount is not equal total product amount : "
                                + "orderAmount - %s , productsTotalAmount - %s ",
                        order.getTotalAmount(),
                        productsTotalAmount));
            }
        }
        var operationTotalAmount = createOrderRequestDto.getTotalAmount();
        var calculatedTotalOrderAmount = createOrderRequestDto.getDeliveryInfo().stream()
                .map(OrderProductDeliveryInfo::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (calculatedTotalOrderAmount.compareTo(operationTotalAmount) != 0) {
            throw new CommonException(Error.NO_MATCH_LOAN_AMOUNT_BY_ORDERS, String.format(
                    "Loan amount is not equal total order amount : loanAmount=%s , "
                            + "totalOrderAmount=%s ", operationTotalAmount,
                    calculatedTotalOrderAmount));
        }
    }

    private void validatePurchaseAmountLimit(CreateOrderRequestDto request) {
        var purchaseAmount = getPurchaseAmount(request);
        var minLimitDifference = purchaseAmount.compareTo(BigDecimal.valueOf(50));
        var maxLimitDifference = purchaseAmount.compareTo(BigDecimal.valueOf(15000));
        if (minLimitDifference < 0 || maxLimitDifference > 0) {
            throw new CommonException(Error.PURCHASE_AMOUNT_LIMIT,
                    "Purchase amount must be between 50 and 15000 "
                            + "in first transaction : purchaseAmount - " + purchaseAmount);
        }
    }

    private BigDecimal getPurchaseAmount(CreateOrderRequestDto request) {
        var totalAmount = request.getTotalAmount();
        var loanTerm = request.getLoanTerm();
        var allOrderCommission = request.getDeliveryInfo().stream()
                .map(order -> commissionUtil.getCommission(order.getTotalAmount(), loanTerm))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return totalAmount.add(allOrderCommission);
    }

    private BigDecimal getAvailableBalance(String cardId) {
        var cardDetailResponse = atlasClient.findCardByUid(cardId, ResultType.ACCOUNT);
        var primaryAccount = cardDetailResponse.getAccounts().stream()
                .filter(x -> x.getStatus() == AccountStatus.OPEN_PRIMARY).findFirst();
        if (primaryAccount.isEmpty()) {
            log.info("Account not found in open primary status : cardId - {}", cardId);
            return BigDecimal.ZERO;
        }
        return primaryAccount.get().getAvailableBalance();
    }
}
