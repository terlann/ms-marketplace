package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constant.AtlasConstant.COMPLETE_PRE_PURCHASE_DESCRIPTION;
import static az.kapitalbank.marketplace.constant.AtlasConstant.COMPLETE_PRE_PURCHASE_FOR_REFUND_DESCRIPTION;
import static az.kapitalbank.marketplace.constant.AtlasConstant.PRE_PURCHASE_DESCRIPTION;
import static az.kapitalbank.marketplace.constant.AtlasConstant.TERMINAL_NAME;
import static az.kapitalbank.marketplace.constant.CommonConstant.CUSTOMER_ID_LOG;
import static az.kapitalbank.marketplace.constant.CommonConstant.CUSTOMER_NOT_FOUND_LOG;
import static az.kapitalbank.marketplace.constant.CommonConstant.CUSTOM_REJECT_REASON_CODES;
import static az.kapitalbank.marketplace.constant.CommonConstant.ORDER_NO_EXCEPTION_LOG;
import static az.kapitalbank.marketplace.constant.CommonConstant.ORDER_NO_LOG;
import static az.kapitalbank.marketplace.constant.CommonConstant.ORDER_NO_REQUEST_LOG;
import static az.kapitalbank.marketplace.constant.CommonConstant.ORDER_NO_RESPONSE_LOG;
import static az.kapitalbank.marketplace.constant.CommonConstant.TELESALES_ORDER_ID_LOG;
import static az.kapitalbank.marketplace.constant.FraudResultStatus.FRAUD_OTHER_PIN_REJECTED_WITH_CURRENT_UMICO_USER_ID;
import static az.kapitalbank.marketplace.constant.FraudResultStatus.FRAUD_OTHER_UMICO_USER_ID_REJECTED_WITH_CURRENT_PIN;
import static az.kapitalbank.marketplace.constant.FraudResultStatus.FRAUD_PIN_AND_UMICO_USER_ID_SUSPICIOUS;
import static az.kapitalbank.marketplace.constant.FraudResultStatus.FRAUD_PIN_SUSPICIOUS;
import static az.kapitalbank.marketplace.constant.FraudResultStatus.FRAUD_UMICO_USER_ID_SUSPICIOUS;
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
import az.kapitalbank.marketplace.client.atlas.model.response.AtlasErrorResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.TransactionInfoResponse;
import az.kapitalbank.marketplace.constant.AccountStatus;
import az.kapitalbank.marketplace.constant.BlacklistType;
import az.kapitalbank.marketplace.constant.Error;
import az.kapitalbank.marketplace.constant.FraudType;
import az.kapitalbank.marketplace.constant.ProcessStatus;
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
import az.kapitalbank.marketplace.entity.BlacklistEntity;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.FraudEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.entity.ProcessStepEntity;
import az.kapitalbank.marketplace.entity.ProductEntity;
import az.kapitalbank.marketplace.exception.CommonException;
import az.kapitalbank.marketplace.exception.DeliveryException;
import az.kapitalbank.marketplace.exception.PaybackException;
import az.kapitalbank.marketplace.mapper.CustomerMapper;
import az.kapitalbank.marketplace.mapper.OperationMapper;
import az.kapitalbank.marketplace.mapper.OrderMapper;
import az.kapitalbank.marketplace.messaging.publisher.FraudCheckPublisher;
import az.kapitalbank.marketplace.repository.BlacklistRepository;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import az.kapitalbank.marketplace.repository.FraudRepository;
import az.kapitalbank.marketplace.repository.OperationRepository;
import az.kapitalbank.marketplace.repository.OrderRepository;
import az.kapitalbank.marketplace.util.CommissionUtil;
import az.kapitalbank.marketplace.util.ParserUtil;
import az.kapitalbank.marketplace.util.RrnUtil;
import feign.FeignException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
    FraudRepository fraudRepository;
    BlacklistRepository blacklistRepository;

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
        operationEntity.setCif(request.getCif());
        operationEntity.setContractNumber(request.getContractNumber());
        if (request.getScoringStatus() == ScoringStatus.REJECTED) {
            var processStatus =
                    ProcessStatus.TELESALES_REJECT_CODE_PREFIX + request.getRejectReasonCode();
            operationEntity.setProcessStatus(processStatus);
            var processStep = ProcessStepEntity.builder().value(processStatus).build();
            operationEntity.setProcessSteps(Collections.singletonList(processStep));
            operationEntity.setScoringStatus(ScoringStatus.REJECTED);
            processStep.setOperation(operationEntity);
            var umicoDecisionStatus = umicoService.sendRejectedDecision(operationEntity.getId());
            operationEntity.setUmicoDecisionStatus(umicoDecisionStatus);
            log.info("Telesales result : Customer was failed telesales process : "
                    + "trackId - {} , scoringStatus - {}", trackId, request.getScoringStatus());
            return;
        }
        var customerEntity = operationEntity.getCustomer();
        telesalesResultApproveProcess(request, operationEntity, customerEntity);
        fraudCaseActions(request, operationEntity, customerEntity);
    }

    private void fraudCaseActions(TelesalesResultRequestDto request,
                                  OperationEntity operationEntity,
                                  CustomerEntity customerEntity) {
        var pin = operationEntity.getPin();
        var umicoUserId = customerEntity.getUmicoUserId();
        var processStatus = operationEntity.getProcessStatus();
        if (processStatus.equals(FRAUD_PIN_AND_UMICO_USER_ID_SUSPICIOUS.name())) {
            if (request.getScoringStatus() == ScoringStatus.REJECTED &&
                    CUSTOM_REJECT_REASON_CODES.contains(request.getRejectReasonCode())) {
                var blacklist =
                        List.of(new BlacklistEntity(BlacklistType.PIN, pin, processStatus),
                                new BlacklistEntity(BlacklistType.UMICO_USER_ID, umicoUserId,
                                        processStatus));
                blacklistRepository.saveAll(blacklist);
                // fin ve umico id send to blacklist
            } else if (request.getScoringStatus() == ScoringStatus.APPROVED) {
                blacklistRepository.deleteByValueIn(Set.of(pin, umicoUserId));
                // fin ve umico id cixar from blacklist
            }
        } else if (operationEntity.getProcessStatus().equals(FRAUD_PIN_SUSPICIOUS.name())) {
            if (request.getScoringStatus() == ScoringStatus.REJECTED &&
                    CUSTOM_REJECT_REASON_CODES.contains(request.getRejectReasonCode())) {
                var blacklist =
                        new BlacklistEntity(BlacklistType.PIN, pin, processStatus);
                blacklistRepository.save(blacklist);
                // fin send to blacklist
            } else if (request.getScoringStatus() == ScoringStatus.APPROVED) {
                blacklistRepository.deleteByValueIn(Set.of(pin));
                // fin cixar from blacklist
            }
        } else if (operationEntity.getProcessStatus()
                .equals(FRAUD_UMICO_USER_ID_SUSPICIOUS.name())) {
            if (request.getScoringStatus() == ScoringStatus.APPROVED) {
                fraudRepository.deleteByValueIn(Set.of(umicoUserId));
            } else if (request.getScoringStatus() == ScoringStatus.REJECTED &&
                    CUSTOM_REJECT_REASON_CODES.contains(request.getRejectReasonCode())) {
                var blacklist = new BlacklistEntity(BlacklistType.UMICO_USER_ID, umicoUserId,
                        processStatus);
                blacklistRepository.save(blacklist);
                var fraud = new FraudEntity(FraudType.PIN, pin, processStatus);
                fraudRepository.save(fraud);
                // umico id send to blacklist, fin fraud liste
            } else {
                var fraud = new FraudEntity(FraudType.PIN, pin, processStatus);
                fraudRepository.save(fraud);
                // fin fraud liste
            }
        } else if (operationEntity.getProcessStatus()
                .equals(FRAUD_OTHER_UMICO_USER_ID_REJECTED_WITH_CURRENT_PIN.name())
                && request.getScoringStatus() == ScoringStatus.REJECTED) {
            if (CUSTOM_REJECT_REASON_CODES.contains(request.getRejectReasonCode())) {
                var blacklist =
                        new BlacklistEntity(BlacklistType.PIN, pin, processStatus);
                blacklistRepository.save(blacklist);
                // fin send to blacklist
            } else {
                var fraud = new FraudEntity(FraudType.PIN, pin, processStatus);
                fraudRepository.save(fraud);
                // fin fraud liste
            }
        } else if (operationEntity.getProcessStatus()
                .equals(FRAUD_OTHER_PIN_REJECTED_WITH_CURRENT_UMICO_USER_ID.name())
                && request.getScoringStatus() == ScoringStatus.REJECTED) {
            if (CUSTOM_REJECT_REASON_CODES.contains(request.getRejectReasonCode())) {
                var blacklistUmicoId =
                        new BlacklistEntity(BlacklistType.UMICO_USER_ID, umicoUserId,
                                processStatus);
                blacklistRepository.save(blacklistUmicoId);
                var pins = operationRepository.getRejectedPinWithCurrentUmicoUserId(umicoUserId);
                var fraudEntities = new ArrayList<FraudEntity>();
                for (String p : pins) {
                    fraudEntities.add(new FraudEntity(FraudType.PIN, p, processStatus));
                }
                fraudRepository.saveAll(fraudEntities);
                // umico id send to blacklist, bu ve evvelki fin fraud liste
            } else {
                var fraud = new FraudEntity(FraudType.UMICO_USER_ID, umicoUserId, processStatus);
                fraudRepository.save(fraud);
                // umico id fraud liste
            }
        }
    }

    private void telesalesResultApproveProcess(TelesalesResultRequestDto request,
                                               OperationEntity operationEntity,
                                               CustomerEntity customerEntity) {
        operationEntity.setUmicoDecisionStatus(APPROVED);
        operationEntity.setScoringStatus(ScoringStatus.APPROVED);
        operationEntity.setLoanContractStartDate(request.getLoanContractStartDate());
        operationEntity.setLoanContractEndDate(request.getLoanContractEndDate());
        operationEntity.setScoredAmount(request.getScoredAmount());
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
        operationEntity = operationRepository.saveAndFlush(operationEntity);
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
        var rrn = RrnUtil.generate();
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
        var rrn = RrnUtil.generate();
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
            orderEntity.setRrn(RrnUtil.generate());
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
        var prePurchaseRequest = PrePurchaseRequest.builder()
                .rrn(order.getRrn()).amount(totalOrderAmount).uid(cardId)
                .description(PRE_PURCHASE_DESCRIPTION + order.getOrderNo()).build();
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
            order.setIsAutoPayback(false);
            completePrePurchaseOrder(order, cardId);
            if (order.getTransactionStatus() == FAIL_IN_COMPLETE_REFUND) {
                log.error("Auto payback was failed complete for refund : orderNo - {}",
                        orderNo);
                return;
            }

            refundOrder(order);
            if (order.getTransactionStatus() == FAIL_IN_REFUND) {
                log.error("Auto payback was failed for refund : orderNo - {}", orderNo);
                return;
            }
            order.setIsAutoPayback(true);
            log.info("Auto payback process was finished : orderNo - {}", orderNo);
        });
    }

    @Transactional
    public void retryPrePurchaseOrder() {
        var operations =
                operationRepository.findAllOperationByTransactionStatus(FAIL_IN_PRE_PURCHASE);
        for (OperationEntity operation : operations) {
            var customer = operation.getCustomer();
            var orders = operation.getOrders();
            var purchasedAmount = BigDecimal.ZERO;
            for (OrderEntity order : orders) {
                purchasedAmount = purchasedAmount.add(getPrePurchasedOrderAmount(customer, order));
            }
            customer.setLastTempAmount(customer.getLastTempAmount().subtract(purchasedAmount));
            var isPrePurchasedAllOrders = orders.stream()
                    .allMatch(order -> order.getTransactionStatus() == PRE_PURCHASE);
            if (operation.getIsOtpOperation() != null
                    && operation.getIsOtpOperation()
                    && isPrePurchasedAllOrders) {
                umicoService.sendPrePurchaseResult(operation.getId());
                log.info("Retry prePurchase result was sent to umico: trackId - {}",
                        operation.getId());
            }
        }
    }

    private BigDecimal getPrePurchasedOrderAmount(CustomerEntity customer,
                                                  OrderEntity order) {
        var purchasedAmount = BigDecimal.ZERO;
        if (order.getTransactionStatus() == FAIL_IN_PRE_PURCHASE) {
            var transactionInfo = findTransactionInfo(order.getRrn(), order.getOrderNo());
            if (transactionInfo.isPresent()) {
                if (transactionInfo.get().isTransactionFound()) {
                    order.setApprovalCode(transactionInfo.get().getApprovalCode());
                    order.setTransactionId(transactionInfo.get().getId().toString());
                    order.setTransactionDate(transactionInfo.get().getTransactionDate());
                    order.setTransactionStatus(PRE_PURCHASE);
                    purchasedAmount = purchasedAmount.add(
                            order.getTotalAmount().add(order.getCommission()));
                } else {
                    var lastTempAmount = prePurchaseOrder(customer.getCardId(), order);
                    if (lastTempAmount.compareTo(BigDecimal.ZERO) == 0) {
                        purchasedAmount = purchasedAmount.add(
                                order.getTotalAmount().add(order.getCommission()));
                    }
                }
            }
        }
        return purchasedAmount;
    }

    private Optional<TransactionInfoResponse> findTransactionInfo(String rrn, String orderNo) {
        try {
            log.info("Atlas transaction info process is started : " + ORDER_NO_REQUEST_LOG,
                    orderNo, "rrn - " + rrn + ", orderNo - " + orderNo);
            var transactionInfoResponse = atlasClient.findTransactionInfo(rrn, TERMINAL_NAME);
            log.info("Atlas transaction info process was finished : " + ORDER_NO_RESPONSE_LOG,
                    orderNo, transactionInfoResponse);
            transactionInfoResponse.setTransactionFound(true);
            return Optional.of(transactionInfoResponse);
        } catch (FeignException e) {
            if (e.status() == 400
                    && ParserUtil.parseTo(e.contentUTF8(), AtlasErrorResponse.class).getCode()
                    .equals("TRANSACTION_NOT_FOUND")) {
                log.error("Atlas transaction info not found: orderNo - {}", orderNo);
                return Optional.of(new TransactionInfoResponse(false));
            }
            log.error("Atlas transaction info process was failed : " + ORDER_NO_EXCEPTION_LOG,
                    orderNo, e);
            return Optional.empty();
        }
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
        var rrn = RrnUtil.generate();
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
            log.error("Atlas refund process was failed : " + ORDER_NO_EXCEPTION_LOG, orderNo, e);
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
