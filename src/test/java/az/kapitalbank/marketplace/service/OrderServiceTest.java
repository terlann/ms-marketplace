package az.kapitalbank.marketplace.service;

import az.kapitalbank.marketplace.client.atlas.AtlasClient;
import az.kapitalbank.marketplace.mapper.CreateOrderMapper;
import az.kapitalbank.marketplace.mapper.CustomerMapper;
import az.kapitalbank.marketplace.mapper.OperationMapper;
import az.kapitalbank.marketplace.mapper.OrderMapper;
import az.kapitalbank.marketplace.messaging.sender.FraudCheckSender;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import az.kapitalbank.marketplace.repository.OperationRepository;
import az.kapitalbank.marketplace.repository.OrderRepository;
import az.kapitalbank.marketplace.util.AmountUtil;
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
    void createOrder() {

//        CreateOrderRequestDto request=CreateOrderRequestDto.builder()
//                .customerInfo(CustomerInfo.builder()
//                        .customerId()
//                        .build())
//                .build();

    }
}
