package com.Shawn.dream_shops.service.order;

import com.Shawn.dream_shops.dto.OrderDto;
import com.Shawn.dream_shops.dto.OrderItemDto;
import com.Shawn.dream_shops.enums.OrderStatus;
import com.Shawn.dream_shops.exceptions.ResourceNotFoundException;
import com.Shawn.dream_shops.model.Cart;
import com.Shawn.dream_shops.model.Order;
import com.Shawn.dream_shops.model.OrderItem;
import com.Shawn.dream_shops.model.Product;
import com.Shawn.dream_shops.repository.OrderRepository;
import com.Shawn.dream_shops.repository.ProductRepository;
import com.Shawn.dream_shops.service.cart.CartService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService implements IOrderService{

    @Autowired
    private final OrderRepository orderRepo;
    @Autowired
    private final ProductRepository productRepo;
    @Autowired
    private final CartService cartService;

    @Transactional
    @Override
    public OrderDto placeOrder(Long userId) { // 每次購買都會把購物車清空
        Cart cart = cartService.getUserCartForUpdate(userId);

        if (cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cart is empty");
        }

        // 會先設置好一些訂單內容
        Order order = createOrder(cart);
        // 把購物車的設定為 訂單項目
        List<OrderItem> orderItemList = createOrderItems(order, cart);

        // 設定此次的訂單
        order.setOrderItems(new HashSet<>(orderItemList));
        order.setTotalAmount(calculateTotalAmount(orderItemList));

        // 成立訂單
        Order savedOrder = orderRepo.save(order);

        // 每次購買完就會把購物車清空
        cart.clearItems();

        return convertToDto(savedOrder);
    }

    private Order createOrder(Cart cart)
    {
        Order order = new Order();
        order.setUser(cart.getUser());
        order.setOrderStatus(OrderStatus.PENDING);
        order.setOrderDate(LocalDateTime.now());
        return order;
    }

    private List<OrderItem> createOrderItems(Order order, Cart cart)
    {
        // 每次購買都會把購物車清空
        return cart.getItems()
                .stream() // 變成 Stream<CartItem>
                .sorted(Comparator.comparing(item -> item.getProduct().getId()))
                .map(cartItem ->
                {   // cartItem 其實就是 product
                    Product product = cartItem.getProduct();
                    int quantity = cartItem.getQuantity();
                    if (quantity < 1 || productRepo.decrementAvailableInventory(product.getId(), quantity) == 0) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT, "Product quantity is unavailable");
                    }

                    // map 結束都必須return 一個物件回傳給一個物件 因為map就是一對一轉換的意思
                    return new OrderItem(
                            order,
                            product,
                            cartItem.getQuantity(),
                            cartItem.getUnitPrice()
                    );
                }) // Stream<CartItem> 轉換成 Stream<OrderItem>。
                .toList(); // toList 就是在把 Stream 轉成 List = 轉換成 List<OrderItem>
    }

    private BigDecimal calculateTotalAmount(List<OrderItem> orderItemList)
    {
        return  orderItemList.stream()
                .map(item -> item.getPrice()
                                           .multiply(new BigDecimal(item.getQuantity())))
                .reduce(BigDecimal.ZERO,BigDecimal::add);

    }

    @Override
    @Transactional(readOnly = true)
    public OrderDto getOrder(Long orderId, Long userId) {
        return orderRepo.findByOrderIdAndUserId(orderId, userId).map(this::convertToDto)
                        .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderDto> getUserOrders(Long userId)
    {
        List<Order> orders = orderRepo.findByUserId(userId);
        return orders.stream().map(this::convertToDto).toList();
    }

    @Override
    public OrderDto convertToDto(Order order)
    {
        OrderDto dto = new OrderDto();
        dto.setId(order.getOrderId());
        dto.setUserId(order.getUser().getId());
        dto.setOrderDate(order.getOrderDate());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setStatus(order.getOrderStatus().name());
        dto.setItems(order.getOrderItems().stream().map(this::toItemDto).toList());
        return dto;
    }

    private OrderItemDto toItemDto(OrderItem item) {
        OrderItemDto dto = new OrderItemDto();
        Product product = item.getProduct();
        dto.setProductId(product.getId());
        dto.setProductName(product.getName());
        dto.setProductBrand(product.getBrand());
        dto.setQuantity(item.getQuantity());
        dto.setPrice(item.getPrice());
        return dto;
    }
}
