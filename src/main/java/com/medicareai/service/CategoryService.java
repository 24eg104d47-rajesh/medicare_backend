package com.medicareai.service;

import com.medicareai.dto.medicine.MedicineCategoryRequest;
import com.medicareai.dto.medicine.MedicineCategoryResponse;
import com.medicareai.entity.MedicineCategory;
import com.medicareai.exception.BadRequestException;
import com.medicareai.exception.ResourceNotFoundException;
import com.medicareai.repository.MedicineCategoryRepository;
import com.medicareai.repository.MedicineRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final MedicineCategoryRepository categoryRepository;
    private final MedicineRepository medicineRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<MedicineCategoryResponse> getActiveCategories() {
        return categoryRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MedicineCategoryResponse> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MedicineCategoryResponse getCategoryById(Long id) {
        MedicineCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));
        return mapToResponse(category);
    }

    @Transactional
    public MedicineCategoryResponse createCategory(MedicineCategoryRequest request, Long staffUserId) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category with name '" + request.getName() + "' already exists");
        }

        MedicineCategory category = MedicineCategory.builder()
                .name(request.getName().trim())
                .description(request.getDescription())
                .active(request.isActive())
                .build();

        MedicineCategory saved = categoryRepository.save(category);
        auditLogService.log(staffUserId, "CATEGORY_CREATED", "MedicineCategory", saved.getId(),
                "Created category: " + saved.getName());

        return mapToResponse(saved);
    }

    @Transactional
    public MedicineCategoryResponse updateCategory(Long id, MedicineCategoryRequest request, Long staffUserId) {
        MedicineCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        if (!category.getName().equalsIgnoreCase(request.getName()) && categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category with name '" + request.getName() + "' already exists");
        }

        category.setName(request.getName().trim());
        category.setDescription(request.getDescription());
        category.setActive(request.isActive());

        MedicineCategory updated = categoryRepository.save(category);
        auditLogService.log(staffUserId, "CATEGORY_UPDATED", "MedicineCategory", updated.getId(),
                "Updated category: " + updated.getName());

        return mapToResponse(updated);
    }

    @Transactional
    public void deleteCategory(Long id, Long staffUserId) {
        MedicineCategory category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + id));

        // Soft delete / deactivate if there are medicines, or hard delete if safe
        category.setActive(false);
        categoryRepository.save(category);
        auditLogService.log(staffUserId, "CATEGORY_DEACTIVATED", "MedicineCategory", id,
                "Deactivated category: " + category.getName());
    }

    private MedicineCategoryResponse mapToResponse(MedicineCategory category) {
        return MedicineCategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .active(category.isActive())
                .createdAt(category.getCreatedAt())
                .build();
    }
}
