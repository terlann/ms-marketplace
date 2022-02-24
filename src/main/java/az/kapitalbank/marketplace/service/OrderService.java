package az.kapitalbank.marketplace.service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.exception.AtlasClientException;
import az.kapitalbank.marketplace.client.atlas.model.request.PurchaseCompleteRequest;
import az.kapitalbank.marketplace.client.atlas.model.request.PurchaseRequest;
import az.kapitalbank.marketplace.constant.AccountStatus;
import az.kapitalbank.marketplace.constant.Currency;
import az.kapitalbank.marketplace.constant.OrderStatus;
import az.kapitalbank.marketplace.constant.ResultType;
import az.kapitalbank.marketplace.constant.TransactionStatus;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
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
import az.kapitalbank.marketplace.exception.CustomerNotCompletedProcessException;
import az.kapitalbank.marketplace.exception.CustomerNotFoundException;
import az.kapitalbank.marketplace.exception.LoanAmountIncorrectException;
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

    @Transactional
    public CreateOrderResponse createOrder(CreateOrderRequestDto request) {
        log.info("create loan process start... Request - [{}]", request);
        validateOrderAmount(request);
        var customerId = request.getCustomerInfo().getCustomerId();
        CustomerEntity customerEntity;
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
                    () -> new CustomerNotFoundException("customerId - " + customerId));
            var pendingCustomer = operationRepository.countByCustomerAndUmicoDecisionStatusIn(customerEntity,
                    List.of(UmicoDecisionStatus.PENDING, UmicoDecisionStatus.PREAPPROVED));
            if (pendingCustomer > 0)
                throw new CustomerNotCompletedProcessException("customerId - " + customerId);
            validateCustomerBalance(request, customerEntity.getCardId());
        }
        OperationEntity operationEntity = operationMapper.toOperationEntity(request);
        operationEntity.setCustomer(customerEntity);

        var operationCommission = BigDecimal.ZERO;
        List<OrderEntity> orderEntities = new ArrayList<>();
        List<ProductEntity> productEntities = new ArrayList<>();
        for (OrderProductDeliveryInfo deliveryInfo : request.getDeliveryInfo()) {
            var commission = getCommission(deliveryInfo.getTotalAmount(), request.getLoanTerm());
            operationCommission = operationCommission.add(commission);
            OrderEntity orderEntity = orderMapper.toOrderEntity(deliveryInfo, commission);
            orderEntity.setOperation(operationEntity);

            for (OrderProductItem orderProductItem : request.getProducts()) {
                if (deliveryInfo.getOrderNo().equals(orderProductItem.getOrderNo())) {
                    ProductEntity productEntity =
                            orderMapper.toProductEntity(orderProductItem, orderEntity.getOrderNo());
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

        var trackId = operationEntity.getId();
        var approvedCustomerCount = operationRepository
                .countByCustomerAndUmicoDecisionStatus(customerEntity, UmicoDecisionStatus.APPROVED);
        if (customerId != null && approvedCustomerCount > 0) {
            var purchasedOrders = new ArrayList<OrderEntity>();
            var cardUid = customerEntity.getCardId();
            for (OrderEntity orderEntity : orderEntities) {
                var rrn = GenerateUtil.rrn();
                var purchaseRequest = PurchaseRequest.builder()
                        .rrn(rrn)
                        .amount(orderEntity.getTotalAmount().add(orderEntity.getCommission()))
                        .description("fee=" + orderEntity.getCommission())
                        .currency(Currency.AZN.getCode())
                        .terminalName(terminalName)
                        .uid(cardUid)
                        .build();
                var purchaseResponse = atlasClient.purchase(purchaseRequest);
                orderEntity.setRrn(rrn);
                orderEntity.setTransactionId(purchaseResponse.getId());
                orderEntity.setApprovalCode(purchaseResponse.getApprovalCode());
                orderEntity.setTransactionStatus(TransactionStatus.PURCHASE);
                purchasedOrders.add(orderEntity);
            }
            orderRepository.saveAll(purchasedOrders);
        } else {
            FraudCheckEvent fraudCheckEvent = createOrderMapper.toOrderEvent(request);
            fraudCheckEvent.setTrackId(trackId);
            customerOrderProducer.sendMessage(fraudCheckEvent);
        }
        return CreateOrderResponse.of(trackId);
    }

    /* Optimus call this */
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

    public List<PurchaseResponseDto> purchase(PurchaseRequestDto request) {
        var customerId = request.getCustomerId();
        var customerEntity = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("customerId - " + customerId));

        List<PurchaseResponseDto> purchaseResponseDtoList = new ArrayList<>();
        var cardId = customerEntity.getCardId();
        var orderNoList = request.getDeliveryOrders().stream()
                .map(DeliveryProductDto::getOrderNo)
                .collect(Collectors.toList());

        var orders = orderRepository.findByOrderNoIn(orderNoList);

        for (var order : orders) {
            var purchaseResponseDto = new PurchaseResponseDto();
            var amount = order.getTotalAmount();
            var commission = order.getCommission();
            var totalPayment = commission.add(amount);
            var rrn = GenerateUtil.rrn();
            var purchaseCompleteRequest = PurchaseCompleteRequest.builder()
                    .id(Integer.valueOf(order.getTransactionId()))
                    .uid(cardId)
                    .amount(totalPayment)
                    .approvalCode(order.getApprovalCode())
                    .currency(Currency.AZN.getCode())
                    .rrn(rrn)
                    .terminalName(terminalName)
                    .installments(order.getOperation().getLoanTerm())
                    .build();

            try {
                var purchaseCompleteResponse = atlasClient.complete(purchaseCompleteRequest);
                order.setTransactionId(purchaseCompleteResponse.getId());
                order.setRrn(rrn);
                order.setTransactionStatus(TransactionStatus.COMPLETE);
                purchaseResponseDto.setStatus(OrderStatus.SUCCESS);
            } catch (AtlasClientException ex) {
                purchaseResponseDto.setStatus(OrderStatus.FAIL);
                order.setTransactionStatus(TransactionStatus.FAIL_IN_COMPLETE);
                order.setTransactionError(ex.getMessage());
                String errorMessager = "Atlas Exception: UUID - %s  ,  code - %s, message - %s";
                log.error(String.format(errorMessager,
                        ex.getUuid(),
                        ex.getCode(),
                        ex.getMessage()));
            }
            purchaseResponseDto.setOrderNo(order.getOrderNo());
            order.setTransactionDate(LocalDateTime.now());
            orderRepository.save(order);
            purchaseResponseDtoList.add(purchaseResponseDto);
        }

        return purchaseResponseDtoList;
    }

    @Transactional
    public PurchaseResponseDto reverse(ReverseRequestDto request) {
        var customerId = request.getCustomerId();
        customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("customerId - " + customerId));

        var orderNo = request.getOrderNo();
        var orderEntity = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new OrderNotFoundException(orderNo));

        PurchaseResponseDto purchaseResponse = new PurchaseResponseDto();
        try {
            var reverseResponse = atlasClient.reverse(orderEntity.getTransactionId());
            purchaseResponse.setStatus(OrderStatus.SUCCESS);
            orderEntity.setTransactionId(reverseResponse.getId());
            orderEntity.setTransactionStatus(TransactionStatus.REVERSE);
        } catch (AtlasClientException ex) {
            purchaseResponse.setStatus(OrderStatus.FAIL);
            orderEntity.setTransactionStatus(TransactionStatus.FAIL_IN_REVERSE);
            String errorMessager = "Atlas Exception: UUID - %s  ,  code - %s, message - %s";
            orderEntity.setTransactionError(String.format(errorMessager,
                    ex.getUuid(),
                    ex.getCode(),
                    ex.getMessage()));
            log.error(errorMessager);
        }
        purchaseResponse.setOrderNo(orderEntity.getOrderNo());
        orderEntity.setTransactionDate(LocalDateTime.now());
        orderRepository.save(orderEntity);
        return purchaseResponse;
    }


    private void validateCustomerBalance(CreateOrderRequestDto request, String cardId) {
        var purchaseAmount = getPurchaseAmount(request);
        var availableBalance = getAvailableBalance(cardId);
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

    private BigDecimal getAvailableBalance(String cardId) {
        var cardDetailResponse = atlasClient.findCardByUID(cardId, ResultType.ACCOUNT);

        var accountResponseList = cardDetailResponse.getAccounts()
                .stream()
                .filter(x -> x.getStatus() == AccountStatus.OPEN_PRIMARY).findFirst()
                .orElseThrow(() -> new RuntimeException("Open Primary Account not Found in Account Response"));
        return accountResponseList.getAvailableBalance();
    }

}