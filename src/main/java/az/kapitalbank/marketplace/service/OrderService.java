package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constant.AtlasConstant.REVERSE_DESCRIPTION;
import static az.kapitalbank.marketplace.constant.CommonConstant.CUSTOMER_NOT_FOUND_BY_ID;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.exception.AtlasClientException;
import az.kapitalbank.marketplace.client.atlas.model.request.PurchaseCompleteRequest;
import az.kapitalbank.marketplace.client.atlas.model.request.ReversePurchaseRequest;
import az.kapitalbank.marketplace.constant.AccountStatus;
import az.kapitalbank.marketplace.constant.OrderStatus;
import az.kapitalbank.marketplace.constant.ResultType;
import az.kapitalbank.marketplace.constant.TransactionStatus;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.dto.DeliveryProductDto;
import az.kapitalbank.marketplace.dto.OrderProductDeliveryInfo;
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
import az.kapitalbank.marketplace.exception.CustomerNotCompletedProcessException;
import az.kapitalbank.marketplace.exception.CustomerNotFoundException;
import az.kapitalbank.marketplace.exception.NoEnoughBalanceException;
import az.kapitalbank.marketplace.exception.NoMatchLoanAmountException;
import az.kapitalbank.marketplace.exception.NoPermissionForTransaction;
import az.kapitalbank.marketplace.exception.OperationAlreadyScoredException;
import az.kapitalbank.marketplace.exception.OperationNotFoundException;
import az.kapitalbank.marketplace.exception.OrderNotFoundException;
import az.kapitalbank.marketplace.exception.OrderNotLinkedToCustomer;
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
    CustomerMapper customerMapper;
    OrderRepository orderRepository;
    OperationMapper operationMapper;
    CustomerRepository customerRepository;
    FraudCheckSender customerOrderProducer;
    OperationRepository operationRepository;

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
            customerEntity = customerRepository.findById(customerId).orElseThrow(
                    () -> new CustomerNotFoundException(CUSTOMER_NOT_FOUND_BY_ID + customerId));
            var isExistsCustomerByDecisionStatus = operationRepository
                    .existsByCustomerAndUmicoDecisionStatusIn(customerEntity,
                            Set.of(UmicoDecisionStatus.PENDING, UmicoDecisionStatus.PREAPPROVED));
            if (isExistsCustomerByDecisionStatus) {
                throw new CustomerNotCompletedProcessException(
                        CUSTOMER_NOT_FOUND_BY_ID + customerId);
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
                () -> new OperationNotFoundException("telesalesOrderId - " + telesalesOrderId));
        var scoringStatus = operationEntity.getScoringStatus();
        if (scoringStatus != null) {
            throw new OperationAlreadyScoredException("telesalesOrderId - " + telesalesOrderId);
        }
        var orderResponseDto = orderMapper.entityToDto(operationEntity);
        log.info("Check order was finished : telesalesOrderId - {}", telesalesOrderId);
        return orderResponseDto;
    }

    @Transactional
    public List<PurchaseResponseDto> purchase(PurchaseRequestDto request) {
        log.info("Purchase process is started : request - {}", request);
        var customerId = request.getCustomerId();
        var customerEntity = customerRepository.findById(customerId)
                .orElseThrow(
                        () -> new CustomerNotFoundException(CUSTOMER_NOT_FOUND_BY_ID + customerId));
        var cardId = customerEntity.getCardId();
        var orderNoList = request.getDeliveryOrders().stream()
                .map(DeliveryProductDto::getOrderNo)
                .collect(Collectors.toList());
        var orders = orderRepository.findByOrderNoIn(orderNoList);
        var purchaseResponseDtoList = new ArrayList<PurchaseResponseDto>();
        for (var order : orders) {
            var transactionStatus = order.getTransactionStatus();
            if (transactionStatus == TransactionStatus.PURCHASE
                    || transactionStatus == TransactionStatus.FAIL_IN_REVERSE
                    || transactionStatus == TransactionStatus.FAIL_IN_COMPLETE) {
                var purchaseResponseDto = purchaseOrder(cardId, order);
                purchaseResponseDtoList.add(purchaseResponseDto);
            }
        }
        log.info("Purchase process was finished : trackId - {}, customerId - {}",
                request.getTrackId(), request.getCustomerId());
        return purchaseResponseDtoList;
    }

    private PurchaseResponseDto purchaseOrder(String cardId, OrderEntity order) {
        var purchaseResponseDto = new PurchaseResponseDto();
        var rrn = GenerateUtil.rrn();
        var purchaseCompleteRequest = getPurchaseCompleteRequest(cardId, order, rrn);
        try {
            var purchaseCompleteResponse = atlasClient
                    .complete(purchaseCompleteRequest);
            var transactionId = purchaseCompleteResponse.getId();
            order.setTransactionId(transactionId);
            order.setTransactionStatus(TransactionStatus.COMPLETE);
            purchaseResponseDto.setStatus(OrderStatus.SUCCESS);
        } catch (AtlasClientException ex) {
            order.setTransactionStatus(TransactionStatus.FAIL_IN_COMPLETE);
            order.setTransactionError(ex.getMessage());
            purchaseResponseDto.setStatus(OrderStatus.FAIL);
            log.error("Atlas complete process was failed. orderNo - {}", order.getOrderNo());
            String errorMessager = "Atlas Exception: UUID - %s  ,  code - %s, message - %s";
            log.error(String.format(errorMessager, ex.getUuid(), ex.getCode(), ex.getMessage()));
        }
        order.setRrn(rrn);
        order.setTransactionDate(LocalDateTime.now());
        orderRepository.save(order);
        purchaseResponseDto.setOrderNo(order.getOrderNo());
        return purchaseResponseDto;
    }

    private PurchaseCompleteRequest getPurchaseCompleteRequest(String cardId, OrderEntity order,
                                                               String rrn) {
        var amount = order.getTotalAmount();
        var commission = order.getCommission();
        var totalPayment = commission.add(amount);
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
                .orElseThrow(() -> new OrderNotFoundException("orderNo - " + orderNo));
        var transactionStatus = orderEntity.getTransactionStatus();
        if (transactionStatus == TransactionStatus.PURCHASE
                || transactionStatus == TransactionStatus.FAIL_IN_REVERSE
                || transactionStatus == TransactionStatus.FAIL_IN_COMPLETE) {
            var customerEntity = orderEntity.getOperation().getCustomer();
            if (!customerEntity.getId().equals(request.getCustomerId())) {
                throw new OrderNotLinkedToCustomer(
                        "orderNo - " + orderNo + ", customerId - " + customerId);
            }
            var purchaseResponse = reverseOrder(orderEntity);
            log.info("Reverse process was finished : orderNo - {}, customerId - {}",
                    request.getOrderNo(), request.getCustomerId());
            return purchaseResponse;
        }
        throw new NoPermissionForTransaction(
                "orderNo- " + orderEntity.getId() + " transactionStatus- "
                        + orderEntity.getTransactionStatus());
    }

    private PurchaseResponseDto reverseOrder(OrderEntity orderEntity) {
        var purchaseResponse = new PurchaseResponseDto();
        try {
            var reverseResponse =
                    atlasClient.reverse(orderEntity.getTransactionId(),
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
            log.error(String.format(errorMessager,
                    ex.getUuid(),
                    ex.getCode(),
                    ex.getMessage()));
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
        var selectedTotalOrderAmount = createOrderRequestDto.getTotalAmount();
        var calculatedTotalOrderAmount = createOrderRequestDto.getDeliveryInfo().stream()
                .map(OrderProductDeliveryInfo::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (calculatedTotalOrderAmount.compareTo(selectedTotalOrderAmount) != 0) {
            throw new NoMatchLoanAmountException(selectedTotalOrderAmount,
                    calculatedTotalOrderAmount);
        }
    }

    private void validatePurchaseAmountLimit(CreateOrderRequestDto request) {
        var purchaseAmount = getPurchaseAmount(request);
        var minLimitAmount = BigDecimal.valueOf(50);
        var maxLimitAmount = BigDecimal.valueOf(20000);
        if (purchaseAmount.compareTo(minLimitAmount) < 0
                || purchaseAmount.compareTo(maxLimitAmount) > 0) {
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
        var primaryAccount = cardDetailResponse.getAccounts()
                .stream()
                .filter(x -> x.getStatus() == AccountStatus.OPEN_PRIMARY)
                .findFirst();
        if (primaryAccount.isEmpty()) {
            log.warn("Account not found in open primary status.cardId - {}", cardId);
            return BigDecimal.ZERO;
        }
        return primaryAccount.get().getAvailableBalance();
    }
}
