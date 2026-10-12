package com.medicareai.controller;

import com.medicareai.dto.cart.AddToCartRequest;
import com.medicareai.dto.cart.CartResponse;
import com.medicareai.dto.cart.UpdateCartItemRequest;
import com.medicareai.dto.common.ApiResponse;
import com.medicareai.security.UserDetailsImpl;
import com.medicareai.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        CartResponse cart = cartService.getCart(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(cart));
    }

    @PostMapping("/items")
    public ResponseEntity<ApiResponse<CartResponse>> addToCart(
            @Valid @RequestBody AddToCartRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        CartResponse cart = cartService.addToCart(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Item added to cart", cart));
    }

    @PatchMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartResponse>> updateCartItem(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCartItemRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        CartResponse cart = cartService.updateCartItemQuantity(userDetails.getId(), id, request.getQuantity());
        return ResponseEntity.ok(ApiResponse.ok("Cart item updated", cart));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<ApiResponse<CartResponse>> removeFromCart(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        CartResponse cart = cartService.removeFromCart(userDetails.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Item removed from cart", cart));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponse<Void>> clearCart(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        cartService.clearCart(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Cart cleared", null));
    }
}
