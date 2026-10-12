package com.medicareai.service;

import com.medicareai.dto.common.PageResponse;
import com.medicareai.dto.medicine.MedicineRequest;
import com.medicareai.dto.medicine.MedicineResponse;
import com.medicareai.entity.Medicine;
import com.medicareai.entity.MedicineBatch;
import com.medicareai.entity.MedicineCategory;
import com.medicareai.exception.ResourceNotFoundException;
import com.medicareai.repository.MedicineBatchRepository;
import com.medicareai.repository.MedicineCategoryRepository;
import com.medicareai.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MedicineService {

    private final MedicineRepository medicineRepository;
    private final MedicineCategoryRepository categoryRepository;
    private final MedicineBatchRepository batchRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public PageResponse<MedicineResponse> getPublicMedicines(
            int page, int size, Long categoryId, String category, String search,
            Boolean prescriptionRequired, String sortParam, String sortBy, String sortDir) {

        // 1. Resolve categoryId if category text/name/id is passed
        Long resolvedCategoryId = categoryId;
        if (resolvedCategoryId == null && category != null && !category.isBlank()) {
            String trimmed = category.trim();
            if (trimmed.matches("\\d+")) {
                resolvedCategoryId = Long.parseLong(trimmed);
            } else {
                resolvedCategoryId = categoryRepository.findByNameIgnoreCase(trimmed)
                        .map(MedicineCategory::getId)
                        .orElse(null);
            }
        }

        // 2. Resolve sorting safely
        String resolvedSortBy = sortBy != null ? sortBy : "id";
        String resolvedSortDir = sortDir != null ? sortDir : "desc";

        if (sortParam != null && !sortParam.isBlank()) {
            String[] parts = sortParam.split(",");
            if (parts.length > 0 && !parts[0].isBlank()) {
                resolvedSortBy = parts[0].trim();
            }
            if (parts.length > 1 && !parts[1].isBlank()) {
                resolvedSortDir = parts[1].trim();
            }
        }

        // Safe sort fields on Medicine entity
        if ("price".equalsIgnoreCase(resolvedSortBy)) {
            resolvedSortBy = "name";
        }
        if (!List.of("id", "name", "brand", "genericName", "createdAt").contains(resolvedSortBy)) {
            resolvedSortBy = "id";
        }

        Sort sort = "desc".equalsIgnoreCase(resolvedSortDir)
                ? Sort.by(resolvedSortBy).descending()
                : Sort.by(resolvedSortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);

        String searchQuery = (search != null && !search.isBlank()) ? search.trim() : null;

        Page<Medicine> pageResult = medicineRepository.filterPublicMedicines(
                resolvedCategoryId,
                searchQuery,
                prescriptionRequired,
                pageable
        );

        List<MedicineResponse> content = pageResult.getContent().stream()
                .map(this::enrichAndMapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<MedicineResponse>builder()
                .content(content)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PageResponse<MedicineResponse> getPublicMedicines(int page, int size, Long categoryId, String search, String sortBy, String sortDir) {
        return getPublicMedicines(page, size, categoryId, null, search, null, null, sortBy, sortDir);
    }

    @Transactional(readOnly = true)
    public PageResponse<MedicineResponse> getAdminMedicines(int page, int size, Long categoryId, String search, Boolean activeOnly) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("id").descending());
        boolean activeFilter = Boolean.TRUE.equals(activeOnly);

        Page<Medicine> pageResult = medicineRepository.filterMedicines(
                categoryId,
                search != null ? search.trim() : "",
                activeFilter,
                pageable
        );

        List<MedicineResponse> content = pageResult.getContent().stream()
                .map(this::enrichAndMapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<MedicineResponse>builder()
                .content(content)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public MedicineResponse getMedicineById(Long id) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + id));
        return enrichAndMapToResponse(medicine);
    }

    @Transactional(readOnly = true)
    public List<MedicineResponse> getPopularMedicines() {
        return medicineRepository.findTop8ByActiveTrueOrderByIdDesc().stream()
                .map(this::enrichAndMapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MedicineResponse createMedicine(MedicineRequest request, Long staffUserId) {
        MedicineCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

        Medicine medicine = Medicine.builder()
                .category(category)
                .name(request.getName().trim())
                .brand(request.getBrand().trim())
                .genericName(request.getGenericName().trim())
                .manufacturer(request.getManufacturer().trim())
                .description(request.getDescription())
                .dosageForm(request.getDosageForm().trim())
                .strength(request.getStrength().trim())
                .packSize(request.getPackSize().trim())
                .productImageUrl(request.getProductImageUrl())
                .requiresPrescription(request.isRequiresPrescription())
                .active(request.isActive())
                .build();

        Medicine saved = medicineRepository.save(medicine);
        auditLogService.log(staffUserId, "MEDICINE_CREATED", "Medicine", saved.getId(),
                "Created medicine: " + saved.getName() + " (" + saved.getBrand() + ")");

        return enrichAndMapToResponse(saved);
    }

    @Transactional
    public MedicineResponse updateMedicine(Long id, MedicineRequest request, Long staffUserId) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + id));

        MedicineCategory category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

        medicine.setCategory(category);
        medicine.setName(request.getName().trim());
        medicine.setBrand(request.getBrand().trim());
        medicine.setGenericName(request.getGenericName().trim());
        medicine.setManufacturer(request.getManufacturer().trim());
        medicine.setDescription(request.getDescription());
        medicine.setDosageForm(request.getDosageForm().trim());
        medicine.setStrength(request.getStrength().trim());
        medicine.setPackSize(request.getPackSize().trim());
        medicine.setProductImageUrl(request.getProductImageUrl());
        medicine.setRequiresPrescription(request.isRequiresPrescription());
        medicine.setActive(request.isActive());

        Medicine updated = medicineRepository.save(medicine);
        auditLogService.log(staffUserId, "MEDICINE_UPDATED", "Medicine", updated.getId(),
                "Updated medicine: " + updated.getName());

        return enrichAndMapToResponse(updated);
    }

    @Transactional
    public MedicineResponse updateMedicineStatus(Long id, boolean active, Long staffUserId) {
        Medicine medicine = medicineRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + id));

        medicine.setActive(active);
        Medicine updated = medicineRepository.save(medicine);

        auditLogService.log(staffUserId, "MEDICINE_STATUS_CHANGED", "Medicine", updated.getId(),
                "Changed status of medicine " + updated.getName() + " to " + (active ? "ACTIVE" : "INACTIVE"));

        return enrichAndMapToResponse(updated);
    }

    public MedicineResponse enrichAndMapToResponse(Medicine medicine) {
        LocalDate today = LocalDate.now();
        List<MedicineBatch> eligibleBatches = batchRepository.findEligibleBatchesForSale(medicine.getId(), today);

        int totalStock = eligibleBatches.stream()
                .mapToInt(MedicineBatch::getAvailableQuantity)
                .sum();

        BigDecimal startingPrice = eligibleBatches.isEmpty()
                ? BigDecimal.ZERO
                : eligibleBatches.get(0).getSellingPrice();

        return MedicineResponse.builder()
                .id(medicine.getId())
                .categoryId(medicine.getCategory().getId())
                .categoryName(medicine.getCategory().getName())
                .name(medicine.getName())
                .brand(medicine.getBrand())
                .genericName(medicine.getGenericName())
                .manufacturer(medicine.getManufacturer())
                .description(medicine.getDescription())
                .dosageForm(medicine.getDosageForm())
                .strength(medicine.getStrength())
                .packSize(medicine.getPackSize())
                .productImageUrl(medicine.getProductImageUrl())
                .requiresPrescription(medicine.isRequiresPrescription())
                .active(medicine.isActive())
                .availableStock(totalStock)
                .price(startingPrice)
                .createdAt(medicine.getCreatedAt())
                .build();
    }
}
