package az.kapitalbank.marketplace.service.impl;

import az.kapitalbank.marketplace.dto.OrderProductItem;
import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.dto.response.CreateOrderResponse;
import az.kapitalbank.marketplace.dto.response.WrapperResponseDto;
import az.kapitalbank.marketplace.entity.OrderEntity;
import az.kapitalbank.marketplace.entity.OrderProductEntity;
import az.kapitalbank.marketplace.exception.models.LoanAmountIncorrectException;
import az.kapitalbank.marketplace.exception.models.OrderAlreadyDeactivatedException;
import az.kapitalbank.marketplace.exception.models.OrderAlreadyScoringException;
import az.kapitalbank.marketplace.exception.models.OrderNotFindException;
import az.kapitalbank.marketplace.mappers.CreateOrderMapper;
import az.kapitalbank.marketplace.mappers.CustomerMapper;
import az.kapitalbank.marketplace.messaging.event.FraudCheckEvent;
import az.kapitalbank.marketplace.messaging.sender.FraudCheckSender;
import az.kapitalbank.marketplace.repository.CustomerRepository;
import az.kapitalbank.marketplace.repository.OrderRepository;
import az.kapitalbank.marketplace.service.OrderService;
import az.kapitalbank.marketplace.utils.AmountUtil;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderServiceImpl implements OrderService {

    OrderRepository orderRepository;
    CustomerRepository customerRepository;

    CustomerMapper customerMapper;
    CreateOrderMapper createOrderMapper;

    FraudCheckSender customerOrderProducer;


    @Transactional
    @Override
    public WrapperResponseDto<Object> createOrder(CreateOrderRequestDto request) {
        log.info("create order start... Request - [{}]", request.toString());
        validateOrder(request);
        OrderEntity orderEntity = saveOrder(request);
        var trackId = orderEntity.getId();
        sendCustomerOrderEvent(request, trackId);
        log.info("create order save marketplace order. track_id - [{}]", trackId);
        CreateOrderResponse createOrderResponse = CreateOrderResponse.of(trackId);
        log.info("create order finish... track_id - [{}]", trackId);
        WrapperResponseDto<Object> response = WrapperResponseDto.ofSuccess();
        response.setData(createOrderResponse);
        return response;
    }

    @Transactional
    public OrderEntity saveOrder(CreateOrderRequestDto createOrderRequestDto) {
        List<OrderProductEntity> orderProductEntityList = orderProductEntityList(createOrderRequestDto);
        OrderEntity orderEntity = createOrderMapper.toOrderEntity(createOrderRequestDto, orderProductEntityList);
        orderEntity.setInsertedDate(LocalDateTime.now());
        orderProductEntityList.forEach(o -> o.setOrder(orderEntity));
        OrderEntity orderEntityResult = orderRepository.saveAndFlush(orderEntity);
        var customerEntity = customerMapper.toCustomerEntity(createOrderRequestDto);
        customerEntity.setOrder(orderEntity);
        customerRepository.save(customerEntity);
        return orderEntityResult;
    }

    public void validateOrder(CreateOrderRequestDto createOrderRequestDto) {
        List<OrderProductEntity> orderProductEntityList = orderProductEntityList(createOrderRequestDto);
        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal loanAmount = AmountUtil.divideAmount(createOrderRequestDto.getTotalAmount());

        for (OrderProductEntity orderProducts : orderProductEntityList) {
            totalAmount = totalAmount.add(orderProducts.getProductAmount());
        }

        if (loanAmount.subtract(totalAmount).doubleValue() != 0) {
            log.error("create order total amount is incorrect . Request - [{}]," +
                            " total amount - [{}],expected amount - [{}]",
                    createOrderRequestDto,
                    loanAmount,
                    totalAmount);
            throw new LoanAmountIncorrectException(totalAmount.toString());
        }
    }

    public List<OrderProductEntity> orderProductEntityList(CreateOrderRequestDto createOrderRequestDto) {
        List<OrderProductItem> orderProductItemList = createOrderRequestDto.getProducts();
        List<OrderProductEntity> productEntityList = new ArrayList<>();
        orderProductItemList
                .forEach(product -> productEntityList.add(createOrderMapper.toProductOrderEntity(product)));
        return productEntityList;
    }

    protected void sendCustomerOrderEvent(CreateOrderRequestDto createOrderRequestDto, String trackId) {
        FraudCheckEvent fraudCheckEvent = createOrderMapper.toOrderEvent(createOrderRequestDto);
        fraudCheckEvent.setTrackId(trackId);
        customerOrderProducer.sendMessage(fraudCheckEvent);
    }

    @Override
    @Transactional
    public WrapperResponseDto<Object> deleteOrder(String trackId) {
        Optional<OrderEntity> marketplaceOrderEntityOptional = orderRepository.findById(trackId);
        log.info("delete order start v2... track_id - [{}]", trackId);
        marketplaceOrderEntityOptional
                .orElseThrow(() -> new OrderNotFindException(String.format("track_id - [%s]", trackId)));
        OrderEntity orderEntity = marketplaceOrderEntityOptional.get();
        if (orderEntity.getIsActive() != 1) {
            throw new OrderAlreadyDeactivatedException(trackId);
        }

        if (orderEntity.getScoringStatus() != null) {
            throw new OrderAlreadyScoringException(trackId.toString());
        }
        log.info("deleting order v2. track_id - [{}]", trackId);
        orderEntity.setIsActive(0);
        orderEntity.setDeactivatedDate(LocalDateTime.now());
        WrapperResponseDto<Object> wrapperResponseDto = WrapperResponseDto.ofSuccess();
        log.info("delete order finish v2... track_id - [{}]", trackId);
        return wrapperResponseDto;
    }


}