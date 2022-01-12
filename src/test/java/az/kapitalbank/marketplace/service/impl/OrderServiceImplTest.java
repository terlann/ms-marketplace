package az.kapitalbank.marketplace.service.impl;

import az.kapitalbank.marketplace.dto.CustomerInfo;
import az.kapitalbank.marketplace.dto.OrderProductItem;
import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.dto.response.CreateOrderResponse;
import az.kapitalbank.marketplace.dto.response.WrapperResponseDto;
import az.kapitalbank.marketplace.entity.CustomerEntity;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.entity.OrderProductEntity;
import az.kapitalbank.marketplace.mappers.CreateOrderMapper;
import az.kapitalbank.marketplace.mappers.CustomerMapper;
import az.kapitalbank.marketplace.messaging.event.FraudCheckEvent;
import az.kapitalbank.marketplace.messaging.sender.FraudCheckSender;
import az.kapitalbank.marketplace.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static az.kapitalbank.marketplace.property.CustomerConstants.FULLNAME;
import static az.kapitalbank.marketplace.property.CustomerConstants.PHONE_NUMBER;
import static az.kapitalbank.marketplace.property.OrderConstants.LOAN_AMOUNT;
import static az.kapitalbank.marketplace.property.OrderConstants.ORDER_NO;
import static az.kapitalbank.marketplace.property.OrderConstants.TRACK_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {
    @Mock
    OrderRepository orderRepository;
    @Mock
    CreateOrderMapper createOrderMapper;
    @Mock
    CustomerMapper customerMapper;
    @Mock
    FraudCheckSender customerOrderProducer;
    @InjectMocks
    OrderServiceImpl orderService;


    @Test
    void whenCreateOrderCall_ThenShouldBeSuccess() {
        List<OrderProductEntity> orderProductEntityList = List.of(generateOrderProductEntity());
        CreateOrderRequestDto createOrderRequestDto = generateCreateOrderRequest();
        OrderEntity orderEntity = generateOrderEntity();

        CreateOrderResponse createOrderResponse = CreateOrderResponse.of(TRACK_ID);

        WrapperResponseDto<Object> wrapperResponseDto = WrapperResponseDto.ofSuccess();
        wrapperResponseDto.setData(createOrderResponse);

        when(orderService.orderProductEntityList(createOrderRequestDto)).thenReturn(orderProductEntityList);
        when(orderService.saveOrder(createOrderRequestDto)).thenReturn(orderEntity);

        doNothing().when(orderService).sendCustomerOrderEvent(createOrderRequestDto, TRACK_ID);

        orderService.createOrder(createOrderRequestDto);

        verify(orderService).saveOrder(createOrderRequestDto);
        verify(orderService).orderProductEntityList(createOrderRequestDto);
        verify(orderService).sendCustomerOrderEvent(createOrderRequestDto, TRACK_ID);
    }

    @Test
    void whenSaveOrderCall_ThenShouldBeSuccess() {
        CreateOrderRequestDto createOrderRequestDto = generateCreateOrderRequest();
        List<OrderProductEntity> orderProductEntityList = List.of(generateOrderProductEntity());
        OrderEntity orderEntity = generateOrderEntity();
        CustomerEntity customerEntity = generateCustomerEntity();

        when(createOrderMapper.toOrderEntity(generateCreateOrderRequest(), orderProductEntityList))
                .thenReturn(orderEntity);
        when(customerMapper.toCustomerEntity(createOrderRequestDto)).thenReturn(customerEntity);
        when(orderRepository.saveAndFlush(orderEntity)).thenReturn(orderEntity);

        OrderEntity orderEntityResult = orderService.saveOrder(createOrderRequestDto);

        assertThat(orderEntityResult).usingRecursiveComparison().isEqualTo(orderEntity);

        verify(createOrderMapper).toOrderEntity(generateCreateOrderRequest(), orderProductEntityList);
        verify(customerMapper).toCustomerEntity(createOrderRequestDto);
        verify(orderRepository).saveAndFlush(orderEntity);
        verify(createOrderMapper).toOrderEntity(generateCreateOrderRequest(), orderProductEntityList);
    }

    @Test
    void whenSendCustomerOrderEventCall_ThenShouldBeSuccess() {
        var customerOrderEvent = generateCustomerOrderEvent();
        var createOrderRequestDto = generateCreateOrderRequest();

        when(createOrderMapper.toOrderEvent(createOrderRequestDto)).thenReturn(customerOrderEvent);
        doNothing().when(customerOrderProducer).sendMessage(customerOrderEvent);

        orderService.sendCustomerOrderEvent(createOrderRequestDto, TRACK_ID);

        verify(createOrderMapper).toOrderEvent(createOrderRequestDto);
    }


    public OrderEntity generateOrderEntity() {
        OrderEntity orderEntity = new OrderEntity();
        orderEntity.setId(TRACK_ID);
        orderEntity.setTotalAmount(new BigDecimal(LOAN_AMOUNT));
        return orderEntity;
    }

    public CustomerEntity generateCustomerEntity() {
        CustomerEntity customerEntity = new CustomerEntity();
        customerEntity.setFullName(FULLNAME);
        customerEntity.setMobileNumber(PHONE_NUMBER);
        return customerEntity;
    }

    public OrderProductEntity generateOrderProductEntity() {
        OrderProductEntity orderProductEntity = new OrderProductEntity();
        orderProductEntity.setId(1000L);
        orderProductEntity.setOrderNo(ORDER_NO);
        orderProductEntity.setProductAmount(new BigDecimal(LOAN_AMOUNT).multiply(new BigDecimal("100")));
        return orderProductEntity;
    }

    public CreateOrderRequestDto generateCreateOrderRequest() {
        return CreateOrderRequestDto.builder()
                .customerInfo(CustomerInfo.builder()
                        .fullname(FULLNAME)
                        .phoneNumber(PHONE_NUMBER)
                        .build())
                .totalAmount(new BigDecimal(LOAN_AMOUNT).multiply(new BigDecimal("100")))
                .products(List.of(OrderProductItem.builder()
                        .orderNo(ORDER_NO)
                        .productAmount(Integer.valueOf(LOAN_AMOUNT))
                        .build()))
                .build();
    }

    public FraudCheckEvent generateCustomerOrderEvent() {
        return FraudCheckEvent.builder()
                .fullname(FULLNAME)
                .mobileNumber(PHONE_NUMBER)
                .trackId(TRACK_ID)
                .build();
    }
}