package com.Shawn.dream_shops.service.cart;

import com.Shawn.dream_shops.model.Cart;

import java.math.BigDecimal;

public interface ICartService {
    Cart getCart(Long id);
    void clearCart(Long id);
    BigDecimal getTotalPrice(Long id);

    Cart getCartForUpdate(Long id);
    Cart getOrCreateCartForUpdate(Long userId);
    Cart getUserCartForUpdate(Long userId);
}
