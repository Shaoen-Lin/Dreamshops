package com.Shawn.dream_shops;

import com.Shawn.dream_shops.model.Product;
import com.Shawn.dream_shops.model.User;
import com.Shawn.dream_shops.repository.ProductRepository;
import com.Shawn.dream_shops.repository.UserRepository;
import com.Shawn.dream_shops.service.cart.ICartItemService;
import com.Shawn.dream_shops.service.order.IOrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ShopIntegrationTests {

    @Autowired private MockMvc mvc;
    @Autowired private ObjectMapper json;
    @Autowired private UserRepository users;
    @Autowired private ProductRepository products;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ICartItemService cartItems;
    @Autowired private IOrderService orders;

    @Test
    void protectsCustomerRecordsAndCatalogWrites() throws Exception {
        Account owner = account();
        Account other = account();
        Product product = product(2);
        Long cartId = cartItems.addCartItem(owner.id(), product.getId(), 1);
        Long orderId = orders.placeOrder(owner.id()).getId();
        String ownerToken = login(owner);
        String otherToken = login(other);

        mvc.perform(get("/api/v1/carts/{id}/my-cart", cartId)).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/carts/{id}/my-cart", cartId).header("Authorization", otherToken))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/carts/{id}/my-cart", cartId).header("Authorization", ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.user.password").doesNotExist());
        mvc.perform(get("/api/v1/orders/{id}/order", orderId).header("Authorization", otherToken))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/orders/{id}/order", orderId).header("Authorization", ownerToken))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.items[0].quantity").value(1));
        mvc.perform(get("/api/v1/users/{id}/user", owner.id()).header("Authorization", otherToken))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/categories/add").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/categories/add").header("Authorization", ownerToken)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(multipart("/api/v1/images/upload").header("Authorization", ownerToken))
                .andExpect(status().isForbidden());
    }

    @Test
    void deletedAccountTokenIsRejectedEvenWhenEmailIsRegisteredAgain() throws Exception {
        Account original = account();
        String token = login(original);
        users.deleteById(original.id());
        Account replacement = saveAccount(original.email());

        mvc.perform(get("/api/v1/users/{id}/user", replacement.id()).header("Authorization", token))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/users/{id}/user", replacement.id()).header("Authorization", login(replacement)))
                .andExpect(status().isOk());
    }

    @Test
    void simultaneousFirstAddsReuseOneCartAndKeepBothQuantities() throws Exception {
        Account buyer = account();
        Product product = product(3);
        CyclicBarrier start = new CyclicBarrier(2);
        Callable<Long> add = () -> {
            start.await(5, TimeUnit.SECONDS);
            return cartItems.addCartItem(buyer.id(), product.getId(), 1);
        };
        try (var pool = Executors.newFixedThreadPool(2)) {
            var first = pool.submit(add);
            var second = pool.submit(add);
            Long cartId = first.get(15, TimeUnit.SECONDS);
            assertThat(second.get(15, TimeUnit.SECONDS)).isEqualTo(cartId);
            assertThat(cartItems.getCartItem(cartId, product.getId()).getQuantity()).isEqualTo(2);
        }
    }

    @Test
    void simultaneousCheckoutOfSameCartCreatesOneOrder() throws Exception {
        Account buyer = account();
        Product product = product(3);
        cartItems.addCartItem(buyer.id(), product.getId(), 1);

        assertThat(concurrentCheckout(buyer.id(), buyer.id())).containsExactlyInAnyOrder(true, false);
        assertThat(orders.getUserOrders(buyer.id())).hasSize(1);
        assertThat(products.findById(product.getId()).orElseThrow().getInventory()).isEqualTo(2);
    }

    @Test
    void separateCartsCannotBothPurchaseLastUnit() throws Exception {
        Account firstBuyer = account();
        Account secondBuyer = account();
        Product product = product(1);
        cartItems.addCartItem(firstBuyer.id(), product.getId(), 1);
        cartItems.addCartItem(secondBuyer.id(), product.getId(), 1);

        assertThat(concurrentCheckout(firstBuyer.id(), secondBuyer.id())).containsExactlyInAnyOrder(true, false);
        assertThat(orders.getUserOrders(firstBuyer.id()).size() + orders.getUserOrders(secondBuyer.id()).size())
                .isEqualTo(1);
        assertThat(products.findById(product.getId()).orElseThrow().getInventory()).isZero();
    }

    @Test
    void inventoryConflictRollsBackPreviousLinesAndKeepsTheCart() {
        Account buyer = account();
        Account otherBuyer = account();
        Product available = product(2);
        Product scarce = product(1);
        cartItems.addCartItem(buyer.id(), available.getId(), 1);
        Long cartId = cartItems.addCartItem(buyer.id(), scarce.getId(), 1);
        cartItems.addCartItem(otherBuyer.id(), scarce.getId(), 1);
        orders.placeOrder(otherBuyer.id());

        assertThatThrownBy(() -> orders.placeOrder(buyer.id()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        failure -> assertThat(failure.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(products.findById(available.getId()).orElseThrow().getInventory()).isEqualTo(2);
        assertThat(orders.getUserOrders(buyer.id())).isEmpty();
        assertThat(cartItems.getCartItem(cartId, available.getId()).getQuantity()).isEqualTo(1);
    }

    private List<Boolean> concurrentCheckout(Long first, Long second) throws Exception {
        CyclicBarrier start = new CyclicBarrier(2);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var firstResult = pool.submit(checkoutAfter(start, first));
            var secondResult = pool.submit(checkoutAfter(start, second));
            return List.of(firstResult.get(15, TimeUnit.SECONDS), secondResult.get(15, TimeUnit.SECONDS));
        }
    }

    private Callable<Boolean> checkoutAfter(CyclicBarrier start, Long userId) {
        return () -> {
            start.await(5, TimeUnit.SECONDS);
            try {
                orders.placeOrder(userId);
                return true;
            } catch (ResponseStatusException failure) {
                assertThat(failure.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                return false;
            }
        };
    }

    private Product product(int inventory) {
        return products.save(new Product("Test " + UUID.randomUUID(), "Test", new BigDecimal("25.00"),
                inventory, "Integration test", null));
    }

    private Account account() {
        return saveAccount(UUID.randomUUID() + "@example.test");
    }

    private Account saveAccount(String email) {
        User user = new User();
        user.setEmail(email);
        user.setFirstName("Integration");
        user.setLastName("Test");
        String password = UUID.randomUUID().toString();
        user.setPassword(passwordEncoder.encode(password));
        return new Account(users.save(user).getId(), email, password);
    }

    private String login(Account account) throws Exception {
        String response = mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", account.email(), "password", account.password()))))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        return "Bearer " + json.readTree(response).path("data").path("token").asText();
    }

    private record Account(Long id, String email, String password) {}
}
