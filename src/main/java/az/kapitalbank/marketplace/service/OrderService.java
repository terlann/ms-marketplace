package az.kapitalbank.marketplace.service;

import az.kapitalbank.marketplace.dto.request.CreateOrderRequestDto;
import az.kapitalbank.marketplace.dto.response.WrapperResponseDto;

public interface OrderService {

    WrapperResponseDto<Object> createOrder(CreateOrderRequestDto request);

    WrapperResponseDto<Object> deleteOrder(String trackId);

}
