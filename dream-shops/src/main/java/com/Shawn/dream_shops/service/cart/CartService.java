package com.Shawn.dream_shops.service.cart;

import com.Shawn.dream_shops.exceptions.ResourceNotFoundException;
import com.Shawn.dream_shops.model.Cart;
import com.Shawn.dream_shops.model.User;
import com.Shawn.dream_shops.repository.CartRepository;
import com.Shawn.dream_shops.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class CartService implements ICartService {

    private final CartRepository cartRepo;
    private final UserRepository userRepo;

    @Override
    public Cart getCart(Long id) {
        return cartRepo.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cart Not Found!"));
    }

    @Override
    @Transactional
    public void clearCart(Long id) {
        getCartForUpdate(id).clearItems();
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getTotalPrice(Long id) {
        return getCart(id).updateTotalAmount();
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Cart getCartForUpdate(Long id) {
        return cartRepo.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Cart Not Found!"));
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Cart getOrCreateCartForUpdate(Long userId) {
        // Lock the user before looking up the cart, including when it does not exist yet.
        User user = lockUser(userId);
        return cartRepo.findByUserIdForUpdate(userId).orElseGet(() -> {
            Cart cart = new Cart();
            cart.setUser(user);
            return cartRepo.save(cart);
        });
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public Cart getUserCartForUpdate(Long userId) {
        lockUser(userId);
        return cartRepo.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user id: " + userId));
    }
    private User lockUser(Long userId) {
        return userRepo.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User Not Found!"));
    }
}
