package az.kapitalbank.marketplace.service;

import static az.kapitalbank.marketplace.constants.TestConstants.CARD_UID;
import static az.kapitalbank.marketplace.constants.TestConstants.CUSTOMER_ID;
import static az.kapitalbank.marketplace.constants.TestConstants.TRACK_ID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.client.atlas.model.response.AccountResponse;
import az.kapitalbank.marketplace.client.atlas.model.response.CardDetailResponse;
import az.kapitalbank.marketplace.constant.AccountStatus;
import az.kapitalbank.marketplace.constant.ResultType;
import az.kapitalbank.marketplace.constant.UmicoDecisionStatus;
import az.kapitalbank.marketplace.dto.OrderProductDeliveryInfo;
import az.kapitalbank.marketplace.dto.OrderProductItem;
import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.dto.request.CustomerInfo;
import az.kapitalbank.marketplace.dto.request.ReverseRequestDto;
import az.kapitalbank.marketplace.dto.response.CreateOrderResponse;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OperationEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.entity.ProductEntity;
import az.kapitalbank.marketplace.mapper.CreateOrderMapper;
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
    CustomerMapper customerMapper;
    @Mock
    OrderRepository orderRepository;
    @Mock
    OperationMapper operationMapper;
    @Mock
    CreateOrderMapper createOrderMapper;
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
        var customerId = UUID.fromString(CUSTOMER_ID.getValue());

        CreateOrderRequestDto createOrderRequestDto =
                getCreateOrderRequestDto(customerId);
        var customerEntity = CustomerEntity.builder()
                .uid(CARD_UID.getValue()).build();
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
        mockCreateOrderFirstOperation(customerId, createOrderRequestDto, customerEntity,
                cardDetailResponse, operationEntity, orderEntity, fraudCheckEvent);
        var actual = orderService.createOrder(createOrderRequestDto);
        var expected = CreateOrderResponse.of(UUID.fromString(TRACK_ID.getValue()));
        assertEquals(expected, actual);
    }

    private CreateOrderRequestDto getCreateOrderRequestDto(UUID customerId) {
        return CreateOrderRequestDto.builder()
                .totalAmount(BigDecimal.valueOf(50))
                .loanTerm(12)
                .customerInfo(CustomerInfo.builder()
                        .customerId(customerId)
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
        when(orderMapper.toProductEntity(OrderProductItem.builder().orderNo("123").build(),
                orderEntity.getOrderNo())).thenReturn(ProductEntity.builder().build());
        when(operationRepository.save(operationEntity)).thenReturn(operationEntity);
        when(createOrderMapper.toOrderEvent(createOrderRequestDto)).thenReturn(fraudCheckEvent);
    }

    void reverse_Success() {
        var reverseRequestDto = ReverseRequestDto.builder().build();
        orderService.reverse(reverseRequestDto);

    }
}
