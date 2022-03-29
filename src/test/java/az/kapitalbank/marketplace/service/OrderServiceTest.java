package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constants.TestConstants.CARD_UID;
import static az.kapitalbank.marketplace.constants.TestConstants.CUSTOMER_ID;
import static az.kapitalbank.marketplace.constants.TestConstants.TRACK_ID;
import static az.kapitalbank.marketplace.constants.TestConstants.UMICO_USER_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.exception.AtlasClientException;
import az.kapitalbank.marketplace.client.atlas.model.request.ReversePurchaseRequest;
import az.kapitalbank.marketplace.client.atlas.model.response.AccountResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.CardDetailResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.PurchaseCompleteResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.ReverseResponse;
import az.kapitalbank.marketplace.constant.AccountStatus;
import az.kapitalbank.marketplace.constant.OrderStatus;
import az.kapitalbank.marketplace.constant.ResultType;
import az.kapitalbank.marketplace.constant.TransactionStatus;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.dto.DeliveryProductDto;
import az.kapitalbank.marketplace.dto.OrderProductDeliveryInfo;
import az.kapitalbank.marketplace.dto.OrderProductItem;
import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.dto.request.CustomerInfo;
import az.kapitalbank.marketplace.dto.request.PurchaseRequestDto;
import az.kapitalbank.marketplace.dto.request.ReverseRequestDto;
import az.kapitalbank.marketplace.dto.response.CheckOrderResponseDto;
import az.kapitalbank.marketplace.dto.response.CreateOrderResponse;
import az.kapitalbank.marketplace.dto.response.PurchaseResponseDto;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
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
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Disabled
@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    AmountUtil amountUtil;
    @Mock
    OrderMapper orderMapper;
    @Mock
    AtlasClient atlasClient;
    @Mock
    CustomerMapper customerMapper;
    @Mock
    OrderRepository orderRepository;
    @Mock
    OperationMapper operationMapper;
    @Mock
    CustomerRepository customerRepository;
    @Mock
    FraudCheckSender customerOrderProducer;
    @Mock
    OperationRepository operationRepository;
    @InjectMocks
    OrderService orderService;

    String terminalName = "R1234567";

    @Test
    void createOrder_firstOperation() {

        CreateOrderRequestDto request =
                getCreateOrderRequestDto(null);
        var customerEntity = CustomerEntity.builder()
                .cardId(CARD_UID.getValue()).build();
        var operationEntity = OperationEntity.builder()
                .id(UUID.fromString(TRACK_ID.getValue()))
                .build();
        var orderEntity = OrderEntity.builder().build();
        var fraudCheckEvent = FraudCheckEvent.builder().build();

        when(customerRepository.findByUmicoUserId(UMICO_USER_ID.getValue()))
                .thenReturn(Optional.empty());
        when(customerMapper.toCustomerEntity(request.getCustomerInfo())).thenReturn(customerEntity);
        when(customerRepository.save(customerEntity)).thenReturn(customerEntity);
        when(amountUtil.getCommission(BigDecimal.valueOf(50), 12))
                .thenReturn(BigDecimal.valueOf(12));
        when(operationMapper.toOperationEntity(request)).thenReturn(operationEntity);
        when(orderMapper.toOrderEntity(request.getDeliveryInfo().get(0),
                BigDecimal.valueOf(12))).thenReturn(orderEntity);
        when(operationRepository.save(operationEntity)).thenReturn(operationEntity);
        when(orderMapper.toOrderEvent(request)).thenReturn(fraudCheckEvent);
        var actual = orderService.createOrder(request);
        var expected = CreateOrderResponse.of(UUID.fromString(TRACK_ID.getValue()));
        assertEquals(expected, actual);
    }

    @Test
    void createOrder_SecondOperation() {
        var customerId = UUID.fromString(CUSTOMER_ID.getValue());

        CreateOrderRequestDto request =
                getCreateOrderRequestDto(customerId);
        var customerEntity = CustomerEntity.builder()
                .cardId(CARD_UID.getValue()).build();
        var cardDetailResponse = CardDetailResponse.builder()
                .accounts(List.of(AccountResponse.builder()
                        .status(AccountStatus.OPEN_PRIMARY)
                        .availableBalance(BigDecimal.valueOf(1000))
                        .build()))
                .build();
        var operationEntity = OperationEntity.builder()
                .id(UUID.fromString(TRACK_ID.getValue()))
                .build();
        var orderEntity = OrderEntity.builder().build();
        var fraudCheckEvent = FraudCheckEvent.builder().build();
        mockCreateOrderFirstOperation(customerId, request, customerEntity,
                cardDetailResponse, operationEntity, orderEntity, fraudCheckEvent);
        var actual = orderService.createOrder(request);
        var expected = CreateOrderResponse.of(UUID.fromString(TRACK_ID.getValue()));
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
                        .build()))

                .build();
    }

    private void mockCreateOrderFirstOperation(UUID customerId,
                                               CreateOrderRequestDto createOrderRequestDto,
                                               CustomerEntity customerEntity,
                                               CardDetailResponse cardDetailResponse,
                                               OperationEntity operationEntity,
                                               OrderEntity orderEntity,
                                               FraudCheckEvent fraudCheckEvent) {
        when(customerRepository.findById(customerId))
                .thenReturn(Optional.of(customerEntity));
        when(operationRepository
                .existsByCustomerAndUmicoDecisionStatusIn(customerEntity,
                        Set.of(UmicoDecisionStatus.PENDING, UmicoDecisionStatus.PREAPPROVED)))
                .thenReturn(false);
        when(amountUtil.getCommission(BigDecimal.valueOf(50), 12))
                .thenReturn(BigDecimal.valueOf(12));
        when(atlasClient.findCardByUid(CARD_UID.getValue(), ResultType.ACCOUNT)).thenReturn(
                cardDetailResponse);
        when(operationMapper.toOperationEntity(createOrderRequestDto)).thenReturn(operationEntity);
        when(orderMapper.toOrderEntity(createOrderRequestDto.getDeliveryInfo().get(0),
                BigDecimal.valueOf(12))).thenReturn(orderEntity);
        when(operationRepository.save(operationEntity)).thenReturn(operationEntity);
    }


    @Test
    void reverse_Success() {
        var reverseRequestDto = ReverseRequestDto.builder()
                .orderNo("12345")
                .customerId(UUID.fromString(CUSTOMER_ID.getValue()))
                .build();
        var orderEntity = OrderEntity.builder()
                .transactionStatus(TransactionStatus.PURCHASE)
                .transactionId("1231564")
                .operation(OperationEntity.builder()
                        .customer(CustomerEntity.builder()
                                .id(UUID.fromString(CUSTOMER_ID.getValue()))
                                .build())
                        .build())
                .build();
        var reverseResponse = ReverseResponse.builder().build();
        var reversPurchaseRequest = ReversePurchaseRequest.builder()
                .description("umico-marketplace reverse operation").build();
        when(orderRepository.findByOrderNo(reverseRequestDto.getOrderNo()))
                .thenReturn(Optional.of(orderEntity));
        when(atlasClient.reverse(orderEntity.getTransactionId(), reversPurchaseRequest))
                .thenReturn(reverseResponse);

        var actual = orderService.reverse(reverseRequestDto);
        var expected = PurchaseResponseDto.builder()
                .status(OrderStatus.SUCCESS)
                .build();
        assertEquals(expected, actual);
    }

    @Test
    void reverse_AtlasClientException() {
        var reverseRequestDto = ReverseRequestDto.builder()
                .orderNo("12345")
                .customerId(UUID.fromString(CUSTOMER_ID.getValue()))
                .build();
        var orderEntity = OrderEntity.builder()
                .transactionStatus(TransactionStatus.PURCHASE)
                .transactionId("1231564")
                .operation(OperationEntity.builder()
                        .customer(CustomerEntity.builder()
                                .id(UUID.fromString(CUSTOMER_ID.getValue()))
                                .build())
                        .build())
                .build();
        var reverseResponse = ReverseResponse.builder().build();
        var reversPurchaseRequest = ReversePurchaseRequest.builder()
                .description("umico-marketplace reverse operation").build();
        when(orderRepository.findByOrderNo(reverseRequestDto.getOrderNo()))
                .thenReturn(Optional.of(orderEntity));
        when(atlasClient.reverse(orderEntity.getTransactionId(), reversPurchaseRequest))
                .thenThrow(new AtlasClientException(null, null, null));

        var actual = orderService.reverse(reverseRequestDto);
        var expected = PurchaseResponseDto.builder()
                .status(OrderStatus.FAIL)
                .build();
        assertEquals(expected, actual);
    }

    @Test
    void purchase_Success() {
        var purchaseRequestDto = PurchaseRequestDto.builder()
                .customerId(UUID.fromString(CUSTOMER_ID.getValue()))
                .deliveryOrders(List.of(DeliveryProductDto.builder().orderNo("123").build()))
                .build();
        CustomerEntity customerEntity = CustomerEntity.builder().build();
        OrderEntity orderEntity = OrderEntity.builder()
                .transactionId("123245")
                .transactionStatus(TransactionStatus.PURCHASE)
                .totalAmount(BigDecimal.valueOf(50))
                .commission(BigDecimal.valueOf(12))
                .operation(OperationEntity.builder().loanTerm(12).build())
                .build();
        var orderNoList = List.of("123");
        var purchaseCompleteResponse =
                PurchaseCompleteResponse.builder().build();

        when(customerRepository.findById(UUID.fromString(CUSTOMER_ID.getValue())))
                .thenReturn(Optional.of(customerEntity));
        when(orderRepository.findByOrderNoIn(orderNoList)).thenReturn(
                List.of(orderEntity));
        when(atlasClient.complete(any())).thenReturn(purchaseCompleteResponse);
        var actual = orderService.purchase(purchaseRequestDto);
        var expected = List.of(PurchaseResponseDto.builder()
                .status(OrderStatus.SUCCESS).build());

        assertEquals(expected, actual);
    }

    @Test
    void purchase_AtlasClientException() {
        var purchaseRequestDto = PurchaseRequestDto.builder()
                .customerId(UUID.fromString(CUSTOMER_ID.getValue()))
                .deliveryOrders(List.of(DeliveryProductDto.builder().orderNo("123").build()))
                .build();
        CustomerEntity customerEntity = CustomerEntity.builder().build();
        OrderEntity orderEntity = OrderEntity.builder()
                .transactionId("123245")
                .transactionStatus(TransactionStatus.PURCHASE)
                .totalAmount(BigDecimal.valueOf(50))
                .commission(BigDecimal.valueOf(12))
                .operation(OperationEntity.builder().loanTerm(12).build())
                .build();
        var orderNoList = List.of("123");
        var purchaseCompleteResponse =
                PurchaseCompleteResponse.builder().build();

        when(customerRepository.findById(UUID.fromString(CUSTOMER_ID.getValue())))
                .thenReturn(Optional.of(customerEntity));
        when(orderRepository.findByOrderNoIn(orderNoList)).thenReturn(
                List.of(orderEntity));
        when(atlasClient.complete(any())).thenThrow(new AtlasClientException(null, null, null));
        var actual = orderService.purchase(purchaseRequestDto);
        var expected = List.of(PurchaseResponseDto.builder()
                .status(OrderStatus.FAIL).build());

        assertEquals(expected, actual);
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
}
