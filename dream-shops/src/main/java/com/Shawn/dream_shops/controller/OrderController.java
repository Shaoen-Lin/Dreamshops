package com.Shawn.dream_shops.controller;

import com.Shawn.dream_shops.dto.OrderDto;
import com.Shawn.dream_shops.exceptions.ResourceNotFoundException;
import com.Shawn.dream_shops.reponse.ApiResponse;
import com.Shawn.dream_shops.service.order.IOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import com.Shawn.dream_shops.security.user.ShopUserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/orders")
public class OrderController {

    @Autowired
    private final IOrderService orderService;

    @PostMapping("/order")
    @PreAuthorize("principal.id == #userId")
    public ResponseEntity<ApiResponse> createOrder(@RequestParam Long userId)
    {
        try {
            OrderDto orderDto = orderService.placeOrder(userId);
            return ResponseEntity.ok(new ApiResponse("Item Order Success", orderDto));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(),null));
        }
    }

    @GetMapping("/{orderId}/order")
    public ResponseEntity<ApiResponse> getOrderById(@PathVariable Long orderId, @AuthenticationPrincipal ShopUserDetails user)
    {
        try {
            OrderDto order = orderService.getOrder(orderId, user.getId());
            return ResponseEntity.ok(new ApiResponse("Success", order));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(),null));
        }
    }

    @GetMapping("/{userId}/orders")
    @PreAuthorize("principal.id == #userId")
    public ResponseEntity<ApiResponse> getUserOrders(@PathVariable Long userId)
    {
        try {
            List<OrderDto> order = orderService.getUserOrders(userId);
            return ResponseEntity.ok(new ApiResponse("Success", order));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(),null));
        }
    }

}
