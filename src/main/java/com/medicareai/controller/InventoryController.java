package com.medicareai.controller;

import com.medicareai.dto.common.ApiResponse;
import com.medicareai.dto.common.PageResponse;
import com.medicareai.dto.inventory.MedicineBatchRequest;
import com.medicareai.dto.inventory.MedicineBatchResponse;
import com.medicareai.dto.inventory.StockAdjustmentRequest;
import com.medicareai.dto.inventory.StockMovementResponse;
import com.medicareai.security.UserDetailsImpl;
import com.medicareai.service.InventoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'PHARMACY_ADMIN')")
public class InventoryController {

    private final InventoryService inventoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MedicineBatchResponse>>> getAllBatches(
            @RequestParam(required = false) Long medicineId) {
        List<MedicineBatchResponse> batches = medicineId != null
                ? inventoryService.getBatchesByMedicine(medicineId)
                : inventoryService.getAllBatches();
        return ResponseEntity.ok(ApiResponse.ok(batches));
    }

    @GetMapping("/low-stock")
    public ResponseEntity<ApiResponse<List<MedicineBatchResponse>>> getLowStockBatches(
            @RequestParam(defaultValue = "20") int threshold) {
        List<MedicineBatchResponse> batches = inventoryService.getLowStockBatches(threshold);
        return ResponseEntity.ok(ApiResponse.ok(batches));
    }

    @GetMapping("/expiring")
    public ResponseEntity<ApiResponse<List<MedicineBatchResponse>>> getExpiringBatches(
            @RequestParam(defaultValue = "90") int days) {
        List<MedicineBatchResponse> batches = inventoryService.getExpiringBatches(days);
        return ResponseEntity.ok(ApiResponse.ok(batches));
    }

    @PostMapping("/stock-receipts")
    public ResponseEntity<ApiResponse<MedicineBatchResponse>> recordStockReceipt(
            @Valid @RequestBody MedicineBatchRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        MedicineBatchResponse created = inventoryService.createBatch(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Stock received and batch created", created));
    }

    @PostMapping("/adjustments")
    public ResponseEntity<ApiResponse<MedicineBatchResponse>> recordStockAdjustment(
            @Valid @RequestBody StockAdjustmentRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        MedicineBatchResponse updated = inventoryService.recordStockAdjustment(request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Stock adjustment recorded", updated));
    }

    @GetMapping("/movements")
    public ResponseEntity<ApiResponse<PageResponse<StockMovementResponse>>> getStockMovements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<StockMovementResponse> movements = inventoryService.getStockMovements(page, size);
        return ResponseEntity.ok(ApiResponse.ok(movements));
    }
}
