package com.Shawn.dream_shops.controller;

import com.Shawn.dream_shops.exceptions.ResourceNotFoundException;
import com.Shawn.dream_shops.reponse.ApiResponse;
import com.Shawn.dream_shops.service.cart.ICartItemService;
import com.Shawn.dream_shops.security.user.ShopUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RequiredArgsConstructor
@RestController
@RequestMapping("${api.prefix}/cartItems")
public class CartItemController {
    @Autowired
    private final ICartItemService cartItemService;

    @PostMapping("/item/add")
    public ResponseEntity<ApiResponse> addItemToCart(@RequestParam Long productId,
                                                     @RequestParam Integer quantity,
                                                     @AuthenticationPrincipal ShopUserDetails user)
    {
        requirePositiveQuantity(quantity);
        try {
            Long cartId = cartItemService.addCartItem(user.getId(), productId, quantity);
            return ResponseEntity.ok(new ApiResponse("Successfully Add Item to Cart id: " + cartId, cartId));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(), null));
        }
    }

    @DeleteMapping("/cart/{cartId}/item/{productId}/remove")
    @PreAuthorize("@cartRepository.existsByIdAndUserId(#cartId, principal.id)")
    public ResponseEntity<ApiResponse> removeItemFromCart(@PathVariable Long cartId, @PathVariable Long productId)
    {
        try {
            cartItemService.removeItemFromCart(cartId, productId);
            return ResponseEntity.ok(new ApiResponse("Remove Item Success",null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(),null));
        }

    }

    @PutMapping("/cart/{cartId}/item/{productId}/update")
    @PreAuthorize("@cartRepository.existsByIdAndUserId(#cartId, principal.id)")
    public ResponseEntity<ApiResponse> updateItemQuantity(@PathVariable Long cartId,
                                                          @PathVariable Long productId,
                                                          @RequestParam Integer quantity)
    {
        requirePositiveQuantity(quantity);
        try {
            cartItemService.updateItemQuantity(cartId, productId, quantity);
            return ResponseEntity.ok(new ApiResponse("Update Item Quantity Success",null));
        } catch (ResourceNotFoundException e) {
            return ResponseEntity.status(NOT_FOUND).body(new ApiResponse(e.getMessage(),null));
        }

    }
    private void requirePositiveQuantity(int quantity) {
        if (quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be positive");
        }
    }
}
