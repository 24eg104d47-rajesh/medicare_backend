package com.medicareai.controller;

import com.medicareai.dto.common.ApiResponse;
import com.medicareai.dto.common.PageResponse;
import com.medicareai.dto.medicine.MedicineCategoryResponse;
import com.medicareai.dto.medicine.MedicineResponse;
import com.medicareai.service.CategoryService;
import com.medicareai.service.MedicineService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MedicineController {

    private final MedicineService medicineService;
    private final CategoryService categoryService;

    @GetMapping("/api/medicines")
    public ResponseEntity<ApiResponse<PageResponse<MedicineResponse>>> getMedicines(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean prescriptionRequired,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PageResponse<MedicineResponse> result = medicineService.getPublicMedicines(
                page, size, categoryId, category, search, prescriptionRequired, sort, sortBy, sortDir);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/api/medicines/{id:[0-9]+}")
    public ResponseEntity<ApiResponse<MedicineResponse>> getMedicineById(@PathVariable Long id) {
        MedicineResponse medicine = medicineService.getMedicineById(id);
        return ResponseEntity.ok(ApiResponse.ok(medicine));
    }

    @GetMapping({"/api/medicines/popular", "/api/medicines/featured"})
    public ResponseEntity<ApiResponse<List<MedicineResponse>>> getPopularMedicines() {
        List<MedicineResponse> popular = medicineService.getPopularMedicines();
        return ResponseEntity.ok(ApiResponse.ok(popular));
    }

    @GetMapping({"/api/medicine-categories", "/api/categories"})
    public ResponseEntity<ApiResponse<List<MedicineCategoryResponse>>> getActiveCategories() {
        List<MedicineCategoryResponse> categories = categoryService.getActiveCategories();
        return ResponseEntity.ok(ApiResponse.ok(categories));
    }

    @GetMapping({"/api/medicine-categories/{id:[0-9]+}", "/api/categories/{id:[0-9]+}"})
    public ResponseEntity<ApiResponse<MedicineCategoryResponse>> getCategoryById(@PathVariable Long id) {
        MedicineCategoryResponse category = categoryService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.ok(category));
    }
}
