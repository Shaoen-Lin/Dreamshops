package com.Shawn.dream_shops.repository;

import com.Shawn.dream_shops.model.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    Optional<Order> findByOrderIdAndUserId(Long orderId, Long userId);
}
