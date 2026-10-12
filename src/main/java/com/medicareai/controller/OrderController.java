package com.medicareai.controller;

import com.medicareai.dto.common.ApiResponse;
import com.medicareai.dto.common.PageResponse;
import com.medicareai.dto.order.*;
import com.medicareai.security.UserDetailsImpl;
import com.medicareai.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/api/orders/checkout")
    public ResponseEntity<ApiResponse<OrderResponse>> checkout(
            @Valid @RequestBody CheckoutRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        OrderResponse order = orderService.checkout(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Order placed successfully", order));
    }

    @GetMapping("/api/orders/mine")
    public ResponseEntity<ApiResponse<List<OrderResponse>>> getMyOrders(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<OrderResponse> orders = orderService.getMyOrders(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(orders));
    }

    @GetMapping("/api/orders/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrderById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        boolean isStaff = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PHARMACY_ADMIN"));
        OrderResponse order = orderService.getOrderById(id, userDetails.getId(), isStaff);
        return ResponseEntity.ok(ApiResponse.ok(order));
    }

    @PostMapping("/api/orders/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @PathVariable Long id,
            @Valid @RequestBody OrderCancelRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        boolean isStaff = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PHARMACY_ADMIN"));
        OrderResponse cancelled = orderService.cancelOrder(id, userDetails.getId(), request.getReason(), isStaff);
        return ResponseEntity.ok(ApiResponse.ok("Order cancelled successfully", cancelled));
    }

    @GetMapping("/api/orders/{id}/history")
    public ResponseEntity<ApiResponse<List<OrderStatusHistoryResponse>>> getOrderHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        boolean isStaff = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PHARMACY_ADMIN"));
        List<OrderStatusHistoryResponse> history = orderService.getOrderHistory(id, userDetails.getId(), isStaff);
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @GetMapping("/api/admin/orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACY_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<OrderResponse> result = orderService.getAllOrders(page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PatchMapping("/api/admin/orders/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACY_ADMIN')")
    public ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusUpdateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        OrderResponse updated = orderService.updateOrderStatus(id, request.getNewStatus(), request.getNotes(), userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Order status updated", updated));
    }
}
