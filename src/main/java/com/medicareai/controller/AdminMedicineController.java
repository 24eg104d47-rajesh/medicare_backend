package com.medicareai.controller;

import com.medicareai.dto.common.ApiResponse;
import com.medicareai.dto.common.PageResponse;
import com.medicareai.dto.medicine.MedicineCategoryRequest;
import com.medicareai.dto.medicine.MedicineCategoryResponse;
import com.medicareai.dto.medicine.MedicineRequest;
import com.medicareai.dto.medicine.MedicineResponse;
import com.medicareai.security.UserDetailsImpl;
import com.medicareai.service.CategoryService;
import com.medicareai.service.MedicineService;
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
public class AdminMedicineController {

    private final MedicineService medicineService;
    private final CategoryService categoryService;

    @GetMapping("/medicines")
    public ResponseEntity<ApiResponse<PageResponse<MedicineResponse>>> getMedicines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean activeOnly) {
        PageResponse<MedicineResponse> result = medicineService.getAdminMedicines(page, size, categoryId, search, activeOnly);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/medicines")
    public ResponseEntity<ApiResponse<MedicineResponse>> createMedicine(
            @Valid @RequestBody MedicineRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        MedicineResponse created = medicineService.createMedicine(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Medicine created successfully", created));
    }

    @PutMapping("/medicines/{id}")
    public ResponseEntity<ApiResponse<MedicineResponse>> updateMedicine(
            @PathVariable Long id,
            @Valid @RequestBody MedicineRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        MedicineResponse updated = medicineService.updateMedicine(id, request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Medicine updated successfully", updated));
    }

    @PatchMapping("/medicines/{id}/status")
    public ResponseEntity<ApiResponse<MedicineResponse>> updateMedicineStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        boolean active = body.getOrDefault("active", true);
        MedicineResponse updated = medicineService.updateMedicineStatus(id, active, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Medicine status updated", updated));
    }

    @DeleteMapping("/medicines/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteMedicine(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        medicineService.updateMedicineStatus(id, false, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Medicine deactivated successfully", null));
    }

    @GetMapping("/medicine-categories")
    public ResponseEntity<ApiResponse<List<MedicineCategoryResponse>>> getAllCategories() {
        List<MedicineCategoryResponse> categories = categoryService.getAllCategories();
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @PostMapping("/medicine-categories")
    public ResponseEntity<ApiResponse<MedicineCategoryResponse>> createCategory(
            @Valid @RequestBody MedicineCategoryRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        MedicineCategoryResponse created = categoryService.createCategory(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Category created successfully", created));
    }

    @PutMapping("/medicine-categories/{id}")
    public ResponseEntity<ApiResponse<MedicineCategoryResponse>> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody MedicineCategoryRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        MedicineCategoryResponse updated = categoryService.updateCategory(id, request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Category updated successfully", updated));
    }

    @DeleteMapping("/medicine-categories/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        categoryService.deleteCategory(id, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Category deactivated successfully", null));
    }
}
