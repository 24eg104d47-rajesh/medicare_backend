package com.medicareai.controller;

import com.medicareai.dto.common.ApiResponse;
import com.medicareai.dto.common.PageResponse;
import com.medicareai.dto.supplier.*;
import com.medicareai.enums.PurchaseOrderStatus;
import com.medicareai.security.UserDetailsImpl;
import com.medicareai.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'PHARMACY_ADMIN')")
public class SupplierController {

    private final SupplierService supplierService;

    @GetMapping("/suppliers")
    public ResponseEntity<ApiResponse<List<SupplierResponse>>> getAllSuppliers(
            @RequestParam(defaultValue = "false") boolean activeOnly) {
        List<SupplierResponse> suppliers = supplierService.getAllSuppliers(activeOnly);
        return ResponseEntity.ok(ApiResponse.ok(suppliers));
    }

    @PostMapping("/suppliers")
    public ResponseEntity<ApiResponse<SupplierResponse>> createSupplier(
            @Valid @RequestBody SupplierRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        SupplierResponse created = supplierService.createSupplier(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Supplier created successfully", created));
    }

    @PutMapping("/suppliers/{id}")
    public ResponseEntity<ApiResponse<SupplierResponse>> updateSupplier(
            @PathVariable Long id,
            @Valid @RequestBody SupplierRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        SupplierResponse updated = supplierService.updateSupplier(id, request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Supplier updated successfully", updated));
    }

    @GetMapping("/purchase-orders")
    public ResponseEntity<ApiResponse<PageResponse<PurchaseOrderResponse>>> getAllPurchaseOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<PurchaseOrderResponse> result = supplierService.getAllPurchaseOrders(page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/purchase-orders/{id}")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> getPurchaseOrderById(@PathVariable Long id) {
        PurchaseOrderResponse po = supplierService.getPurchaseOrderById(id);
        return ResponseEntity.ok(ApiResponse.ok(po));
    }

    @PostMapping("/purchase-orders")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        PurchaseOrderResponse created = supplierService.createPurchaseOrder(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Purchase order created", created));
    }

    @PatchMapping("/purchase-orders/{id}/status")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> updatePurchaseOrderStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        PurchaseOrderStatus status = PurchaseOrderStatus.valueOf(body.get("status"));
        PurchaseOrderResponse updated = supplierService.updatePurchaseOrderStatus(id, status, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Purchase order status updated", updated));
    }

    @PostMapping("/purchase-orders/{id}/receive")
    public ResponseEntity<ApiResponse<PurchaseOrderResponse>> receivePurchaseOrder(
            @PathVariable Long id,
            @Valid @RequestBody ReceivePurchaseOrderRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        PurchaseOrderResponse received = supplierService.receivePurchaseOrder(id, request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Purchase order received and stock batches recorded", received));
    }
}
