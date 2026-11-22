package com.Shawn.dream_shops.service.order;

import com.Shawn.dream_shops.dto.OrderDto;
import com.Shawn.dream_shops.model.Order;

import java.util.List;

public interface IOrderService {
    OrderDto placeOrder(Long userId);
    OrderDto getOrder(Long orderId, Long userId);

    List<OrderDto> getUserOrders(Long userId);

    OrderDto convertToDto(Order order);
}
