package com.Shawn.dream_shops.service.cart;

import com.Shawn.dream_shops.exceptions.ResourceNotFoundException;
import com.Shawn.dream_shops.model.Cart;
import com.Shawn.dream_shops.model.CartItem;
import com.Shawn.dream_shops.model.Product;
import com.Shawn.dream_shops.service.product.IProductService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;


@Service
@RequiredArgsConstructor
public class CartItemService implements ICartItemService{

    @Autowired
    private final IProductService ProductService;
    @Autowired
    private final ICartService CartService;

    @Transactional
    @Override
    public Long addCartItem(Long userId, Long productId, int quantity) {
        Cart cart = CartService.getOrCreateCartForUpdate(userId);
        Product product = ProductService.getProductById(productId);
        CartItem cartItem = cart.getItems()
                                .stream()
                                .filter(item -> item.getProduct().getId().equals(productId))
                                .findFirst()
                                .orElse(new CartItem());

        long nextQuantity = (long) cartItem.getQuantity() + quantity;
        requireAvailableQuantity(product, nextQuantity);

        if (cartItem.getId() == null) {
            cartItem.setProduct(product);
            cartItem.setUnitPrice(product.getPrice());
        }
        cartItem.setQuantity((int) nextQuantity);
        cartItem.setTotalPrice();
        cart.addItem(cartItem);
        return cart.getId();
    }

    @Transactional
    @Override
    public void removeItemFromCart(Long cartId, Long productId) {
        Cart cart = CartService.getCartForUpdate(cartId);
        CartItem itemToRemove = findCartItem(cart, productId);
        cart.removeItem(itemToRemove);
    }

    @Transactional
    @Override
    public void updateItemQuantity(Long cartId, Long productId, int quantity) {
        Cart cart = CartService.getCartForUpdate(cartId);

        CartItem item = findCartItem(cart, productId);
        requireAvailableQuantity(item.getProduct(), quantity);
        item.setQuantity(quantity);
        item.setTotalPrice();

        cart.updateTotalAmount();
    }

    @Transactional
    @Override
    public CartItem getCartItem(Long cartId, Long productId)
    {
        return findCartItem(CartService.getCart(cartId), productId);
    }

    private CartItem findCartItem(Cart cart, Long productId) {
        return cart.getItems()
                .stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Item Not Found"));
    }
    private void requireAvailableQuantity(Product product, long quantity) {
        if (quantity < 1 || quantity > product.getInventory()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Product quantity is unavailable");
        }
    }
}
