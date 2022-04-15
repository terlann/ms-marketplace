package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constants.ConstantObject.getCardDetailResponse;
import static az.kapitalbank.marketplace.constants.ConstantObject.getCustomerEntity;
import static az.kapitalbank.marketplace.constants.ConstantObject.getOperationEntity;
import static az.kapitalbank.marketplace.constants.ConstantObject.getOrderEntity;
import static az.kapitalbank.marketplace.constants.ConstantObject.getProductEntity;
import static az.kapitalbank.marketplace.constants.TestConstants.CARD_UID;
import static az.kapitalbank.marketplace.constants.TestConstants.CUSTOMER_ID;
import static az.kapitalbank.marketplace.constants.TestConstants.TELESALES_ORDER_ID;
import static az.kapitalbank.marketplace.constants.TestConstants.TRACK_ID;
import static az.kapitalbank.marketplace.constants.TestConstants.UMICO_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.exception.AtlasClientException;
import az.kapitalbank.marketplace.client.atlas.model.request.CompletePrePurchaseRequest;
import az.kapitalbank.marketplace.client.atlas.model.request.PrePurchaseRequest;
import az.kapitalbank.marketplace.client.atlas.model.request.RefundRequest;
import az.kapitalbank.marketplace.client.atlas.model.response.CompletePrePurchaseResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.PrePurchaseResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.RefundResponse;
import az.kapitalbank.marketplace.constant.ResultType;
import az.kapitalbank.marketplace.constant.ScoringStatus;
import az.kapitalbank.marketplace.constant.TransactionStatus;
import az.kapitalbank.marketplace.dto.DeliveryProductDto;
import az.kapitalbank.marketplace.dto.OrderProductDeliveryInfo;
import az.kapitalbank.marketplace.dto.OrderProductItem;
import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.dto.request.CustomerInfo;
import az.kapitalbank.marketplace.dto.request.PurchaseRequestDto;
import az.kapitalbank.marketplace.dto.request.RefundRequestDto;
import az.kapitalbank.marketplace.dto.request.TelesalesResultRequestDto;
import az.kapitalbank.marketplace.dto.response.CheckOrderResponseDto;
import az.kapitalbank.marketplace.dto.response.CreateOrderResponse;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.exception.NoMatchOrderAmountByProductException;
import az.kapitalbank.marketplace.exception.NoPermissionForTransaction;
import az.kapitalbank.marketplace.mapper.CustomerMapper;
import az.kapitalbank.marketplace.mapper.OperationMapper;
import az.kapitalbank.marketplace.mapper.OrderMapper;
import az.kapitalbank.marketplace.messaging.event.FraudCheckEvent;
import az.kapitalbank.marketplace.messaging.sender.FraudCheckSender;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import az.kapitalbank.marketplace.repository.OperationRepository;
import az.kapitalbank.marketplace.repository.OrderRepository;
import az.kapitalbank.marketplace.util.AmountUtil;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    AmountUtil amountUtil;
    @Mock
    OrderMapper orderMapper;
    @Mock
    AtlasClient atlasClient;
    @Mock
    UmicoService umicoService;
    @Mock
    CustomerMapper customerMapper;
    @Mock
    OrderRepository orderRepository;
    @Mock
    OperationMapper operationMapper;
    @Mock
    CustomerService customerService;
    @Mock
    CustomerRepository customerRepository;
    @Mock
    FraudCheckSender customerOrderProducer;
    @Mock
    OperationRepository operationRepository;
    @InjectMocks
    OrderService orderService;

    @Test
    void telesalesResult_Rejected() {
        var request = TelesalesResultRequestDto.builder()
                .telesalesOrderId(TELESALES_ORDER_ID.getValue())
                .scoringStatus(ScoringStatus.REJECTED)
                .build();
        when(operationRepository.findByTelesalesOrderId(TELESALES_ORDER_ID.getValue())).thenReturn(
                Optional.of(getOperationEntity()));
        when(umicoService.sendRejectedDecision(getOperationEntity().getId())).thenReturn(
                Optional.empty());

        orderService.telesalesResult(request);
        verify(umicoService).sendRejectedDecision(getOperationEntity().getId());

    }

    @Test
    void telesalesResult_Approved() {
        var request = TelesalesResultRequestDto.builder()
                .telesalesOrderId(TELESALES_ORDER_ID.getValue())
                .scoringStatus(ScoringStatus.APPROVED)
                .uid(CARD_UID.getValue())
                .build();
        var purchaseResponse = PrePurchaseResponse.builder().build();
        when(operationRepository.findByTelesalesOrderId(TELESALES_ORDER_ID.getValue())).thenReturn(
                Optional.of(getOperationEntity()));
        when(atlasClient.prePurchase(any(PrePurchaseRequest.class))).thenReturn(purchaseResponse);
        orderService.telesalesResult(request);
        verify(atlasClient).prePurchase(any(PrePurchaseRequest.class));
    }

    @Test
    void createOrder_firstOperation() {
        CreateOrderRequestDto request =
                getCreateOrderRequestDto(null);
        var fraudCheckEvent = FraudCheckEvent.builder().build();

        when(customerRepository.findByUmicoUserId(UMICO_USER_ID.getValue()))
                .thenReturn(Optional.empty());
        when(amountUtil.getCommission(BigDecimal.valueOf(50), 12))
                .thenReturn(BigDecimal.valueOf(12));
        when(customerMapper.toCustomerEntity(request.getCustomerInfo())).thenReturn(
                getCustomerEntity());
        when(customerRepository.save(any(CustomerEntity.class))).thenReturn(getCustomerEntity());
        when(operationMapper.toOperationEntity(request)).thenReturn(getOperationEntity());
        when(orderMapper.toOrderEntity(request.getDeliveryInfo().get(0),
                BigDecimal.valueOf(12))).thenReturn(getOrderEntity());
        when(orderMapper.toProductEntity(request.getProducts().get(0))).thenReturn(
                getProductEntity());
        when(operationRepository.save(any(OperationEntity.class))).thenReturn(getOperationEntity());
        when(orderMapper.toOrderEvent(request)).thenReturn(fraudCheckEvent);

        var actual = orderService.createOrder(request);
        var expected = CreateOrderResponse.of(UUID.fromString(TRACK_ID.getValue()));
        assertEquals(expected, actual);
        verify(orderMapper).toOrderEvent(request);
    }

    @Test
    void createOrder_SecondOperation() {
        CreateOrderRequestDto request =
                getCreateOrderRequestDto(UUID.fromString("d2a9d8bc-9beb-11ec-b909-0242ac120002"));
        var fraudCheckEvent = FraudCheckEvent.builder().build();

        when(amountUtil.getCommission(BigDecimal.valueOf(50), 12))
                .thenReturn(BigDecimal.valueOf(12));
        when(operationMapper.toOperationEntity(request)).thenReturn(getOperationEntity());
        when(orderMapper.toOrderEntity(request.getDeliveryInfo().get(0),
                BigDecimal.valueOf(12))).thenReturn(getOrderEntity());
        when(orderMapper.toProductEntity(request.getProducts().get(0))).thenReturn(
                getProductEntity());
        when(operationRepository.save(any(OperationEntity.class))).thenReturn(getOperationEntity());
        when(customerRepository.findById(request.getCustomerInfo().getCustomerId())).thenReturn(
                Optional.of(getCustomerEntity()));
        when(atlasClient.findCardByUid(CARD_UID.getValue(), ResultType.ACCOUNT)).thenReturn(
                getCardDetailResponse());
        orderService.createOrder(request);
        verify(operationMapper).toOperationEntity(request);
    }

    @Test
    void createOrder_NoMatchOrderAmountByProductException() {
        CreateOrderRequestDto request =
                getCreateOrderRequestDtoFailInProductAmount(null);
        assertThrows(NoMatchOrderAmountByProductException.class,
                () -> orderService.createOrder(request));
    }

    @Test
    void refund_Success() {
        var refundRequestDto = RefundRequestDto.builder()
                .orderNo("12345")
                .customerId(UUID.fromString(CUSTOMER_ID.getValue()))
                .build();
        var orderEntity = OrderEntity.builder()
                .commission(BigDecimal.ONE)
                .totalAmount(BigDecimal.ONE)
                .transactionStatus(TransactionStatus.PRE_PURCHASE)
                .transactionId("1231564")
                .operation(OperationEntity.builder()
                        .customer(CustomerEntity.builder()
                                .id(UUID.fromString(CUSTOMER_ID.getValue()))
                                .build())
                        .build())
                .build();
        var refundResponse = RefundResponse.builder().build();

        when(orderRepository.findByOrderNo(refundRequestDto.getOrderNo()))
                .thenReturn(Optional.of(orderEntity));
        when(atlasClient.completePrePurchase(any(CompletePrePurchaseRequest.class))).thenReturn(
                CompletePrePurchaseResponse.builder().build());
        when(atlasClient.refund(eq(null), any(RefundRequest.class)))
                .thenReturn(refundResponse);

        orderService.refund(refundRequestDto);
        verify(orderRepository).findByOrderNo(refundRequestDto.getOrderNo());
    }

    @Test
    void refund_AtlasClientException() {
        var refundRequestDto = RefundRequestDto.builder()
                .orderNo("12345")
                .customerId(UUID.fromString(CUSTOMER_ID.getValue()))
                .build();
        var orderEntity = OrderEntity.builder()
                .commission(BigDecimal.ONE)
                .totalAmount(BigDecimal.ONE)
                .transactionStatus(TransactionStatus.PRE_PURCHASE)
                .transactionId("1231564")
                .operation(OperationEntity.builder()
                        .customer(CustomerEntity.builder()
                                .id(UUID.fromString(CUSTOMER_ID.getValue()))
                                .build())
                        .build())
                .build();
        when(orderRepository.findByOrderNo(refundRequestDto.getOrderNo()))
                .thenReturn(Optional.of(orderEntity));
        when(atlasClient.completePrePurchase(any(CompletePrePurchaseRequest.class))).thenReturn(
                CompletePrePurchaseResponse.builder().build());
        when(atlasClient.refund(eq(null), any(RefundRequest.class)))
                .thenThrow(new AtlasClientException(null, null, null));

        assertThrows(AtlasClientException.class, () -> orderService.refund(refundRequestDto));
    }

    @Test
    void purchase_Success() {
        OrderEntity orderEntity = OrderEntity.builder()
                .transactionId("123245")
                .transactionStatus(TransactionStatus.PRE_PURCHASE)
                .totalAmount(BigDecimal.valueOf(50))
                .commission(BigDecimal.valueOf(12))
                .operation(OperationEntity.builder().loanTerm(12).build())
                .products(List.of(getProductEntity()))
                .operation(OperationEntity.builder()
                        .customer(CustomerEntity.builder().cardId(CARD_UID.getValue()).build())
                        .build())
                .build();
        var purchaseCompleteResponse =
                CompletePrePurchaseResponse.builder().build();
        when(orderRepository.findByOrderNo("123")).thenReturn(Optional.of(orderEntity));
        when(amountUtil.getCommissionByPercent(BigDecimal.ONE, null)).thenReturn(
                BigDecimal.ONE);
        when(atlasClient.completePrePurchase(any())).thenReturn(purchaseCompleteResponse);
        var request = PurchaseRequestDto.builder()
                .customerId(UUID.fromString(CUSTOMER_ID.getValue()))
                .orderNo("123")
                .deliveryProducts(Set.of(DeliveryProductDto.builder().productId("p1").build()))
                .build();
        orderService.purchase(request);
        verify(orderRepository).findByOrderNo("123");
    }

    @Test
    void purchase_AlreadyPurchased() {
        OrderEntity orderEntity = OrderEntity.builder()
                .transactionId("123245")
                .transactionStatus(TransactionStatus.COMPLETE_PRE_PURCHASE)
                .totalAmount(BigDecimal.valueOf(50))
                .commission(BigDecimal.valueOf(12))
                .operation(OperationEntity.builder().loanTerm(12).build())
                .products(List.of(getProductEntity()))
                .operation(OperationEntity.builder()
                        .customer(CustomerEntity.builder().cardId(CARD_UID.getValue()).build())
                        .build())
                .build();
        when(orderRepository.findByOrderNo("123")).thenReturn(Optional.of(orderEntity));
        var request = PurchaseRequestDto.builder()
                .customerId(UUID.fromString(CUSTOMER_ID.getValue()))
                .orderNo("123")
                .deliveryProducts(Set.of(DeliveryProductDto.builder().productId("p1").build()))
                .build();
        assertThrows(NoPermissionForTransaction.class, () -> orderService.purchase(request));
        verify(orderRepository).findByOrderNo("123");
    }

    @Test
    void purchase_AtlasClientException() {
        OrderEntity orderEntity = OrderEntity.builder()
                .orderNo("123")
                .transactionId("123245")
                .transactionStatus(TransactionStatus.PRE_PURCHASE)
                .totalAmount(BigDecimal.valueOf(50))
                .commission(BigDecimal.valueOf(12))
                .products(List.of(getProductEntity()))
                .operation(getOperationEntity())
                .build();

        when(orderRepository.findByOrderNo("123")).thenReturn(Optional.of(orderEntity));
        when(amountUtil.getCommissionByPercent(BigDecimal.ONE, null)).thenReturn(BigDecimal.ONE);
        when(atlasClient.completePrePurchase(any())).thenThrow(
                new AtlasClientException(null, null, null));
        var request = PurchaseRequestDto.builder()
                .customerId(UUID.fromString(CUSTOMER_ID.getValue()))
                .orderNo("123")
                .deliveryProducts(Set.of(DeliveryProductDto.builder().productId("p1").build()))
                .build();
        assertThrows(AtlasClientException.class, () -> orderService.purchase(request));
    }

    @Test
    void checkOrder_Success() {
        String telesalesOrderId = "1321651";
        OperationEntity operationEntity = OperationEntity.builder()
                .build();
        var expected = CheckOrderResponseDto.builder().build();
        when(operationRepository.findByTelesalesOrderId(telesalesOrderId))
                .thenReturn(Optional.of(operationEntity));
        when(orderMapper.entityToDto(operationEntity)).thenReturn(expected);
        var actual = orderService.checkOrder(telesalesOrderId);
        assertEquals(expected, actual);
    }

    private CreateOrderRequestDto getCreateOrderRequestDto(UUID customerId) {
        return CreateOrderRequestDto.builder()
                .totalAmount(BigDecimal.valueOf(50))
                .loanTerm(12)
                .customerInfo(CustomerInfo.builder()
                        .customerId(customerId)
                        .umicoUserId(UMICO_USER_ID.getValue())
                        .additionalPhoneNumber1("9941112233")
                        .additionalPhoneNumber2("9941112234")
                        .build())
                .deliveryInfo(List.of(OrderProductDeliveryInfo.builder()
                        .totalAmount(BigDecimal.valueOf(50))
                        .orderNo("123")
                        .build()))
                .products(List.of(OrderProductItem.builder()
                        .orderNo("123")
                        .productAmount(BigDecimal.valueOf(50))
                        .build()))
                .build();
    }

    private CreateOrderRequestDto getCreateOrderRequestDtoFailInProductAmount(UUID customerId) {
        return CreateOrderRequestDto.builder()
                .totalAmount(BigDecimal.valueOf(50))
                .loanTerm(12)
                .customerInfo(CustomerInfo.builder()
                        .customerId(customerId)
                        .umicoUserId(UMICO_USER_ID.getValue())
                        .additionalPhoneNumber1("9941112233")
                        .additionalPhoneNumber2("9941112234")
                        .build())
                .deliveryInfo(List.of(OrderProductDeliveryInfo.builder()
                        .totalAmount(BigDecimal.valueOf(50))
                        .orderNo("123")
                        .build()))
                .products(List.of(OrderProductItem.builder()
                        .orderNo("123")
                        .productAmount(BigDecimal.valueOf(49))
                        .build()))
                .build();
    }

}
