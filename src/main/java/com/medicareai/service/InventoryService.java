package com.medicareai.service;

import com.medicareai.dto.common.PageResponse;
import com.medicareai.dto.inventory.MedicineBatchRequest;
import com.medicareai.dto.inventory.MedicineBatchResponse;
import com.medicareai.dto.inventory.StockAdjustmentRequest;
import com.medicareai.dto.inventory.StockMovementResponse;
import com.medicareai.entity.Medicine;
import com.medicareai.entity.MedicineBatch;
import com.medicareai.entity.StockMovement;
import com.medicareai.enums.StockMovementType;
import com.medicareai.exception.BadRequestException;
import com.medicareai.exception.InventoryException;
import com.medicareai.exception.ResourceNotFoundException;
import com.medicareai.repository.MedicineBatchRepository;
import com.medicareai.repository.MedicineRepository;
import com.medicareai.repository.StockMovementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final MedicineBatchRepository batchRepository;
    private final MedicineRepository medicineRepository;
    private final StockMovementRepository movementRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<MedicineBatchResponse> getAllBatches() {
        return batchRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MedicineBatchResponse> getBatchesByMedicine(Long medicineId) {
        return batchRepository.findByMedicineId(medicineId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MedicineBatchResponse> getLowStockBatches(int threshold) {
        LocalDate today = LocalDate.now();
        return batchRepository.findLowStockBatches(threshold, today).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MedicineBatchResponse> getExpiringBatches(int days) {
        LocalDate today = LocalDate.now();
        LocalDate target = today.plusDays(days);
        return batchRepository.findExpiringBatches(today, target).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public MedicineBatchResponse createBatch(MedicineBatchRequest request, Long staffUserId) {
        Medicine medicine = medicineRepository.findById(request.getMedicineId())
                .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + request.getMedicineId()));

        if (request.getExpiryDate().isBefore(request.getManufacturingDate())) {
            throw new BadRequestException("Expiry date cannot be before manufacturing date");
        }

        if (request.getExpiryDate().isBefore(LocalDate.now())) {
            throw new BadRequestException("Cannot create a batch that is already expired");
        }

        if (batchRepository.findByMedicineIdAndBatchNumber(request.getMedicineId(), request.getBatchNumber()).isPresent()) {
            throw new BadRequestException("Batch number '" + request.getBatchNumber() + "' already exists for this medicine");
        }

        MedicineBatch batch = MedicineBatch.builder()
                .medicine(medicine)
                .batchNumber(request.getBatchNumber().trim())
                .manufacturingDate(request.getManufacturingDate())
                .expiryDate(request.getExpiryDate())
                .purchasePrice(request.getPurchasePrice())
                .sellingPrice(request.getSellingPrice())
                .quantityReceived(request.getQuantityReceived())
                .availableQuantity(request.getQuantityReceived())
                .build();

        MedicineBatch savedBatch = batchRepository.save(batch);

        // Record stock movement
        StockMovement movement = StockMovement.builder()
                .batch(savedBatch)
                .movementType(StockMovementType.STOCK_RECEIVED)
                .quantity(request.getQuantityReceived())
                .referenceType("MANUAL_RECEIPT")
                .referenceId("BATCH-" + savedBatch.getId())
                .notes("Initial batch stock receipt")
                .createdByUserId(staffUserId)
                .build();
        movementRepository.save(movement);

        auditLogService.log(staffUserId, "BATCH_CREATED", "MedicineBatch", savedBatch.getId(),
                "Added batch " + savedBatch.getBatchNumber() + " with qty " + savedBatch.getQuantityReceived());

        return mapToResponse(savedBatch);
    }

    @Transactional
    public MedicineBatchResponse recordStockAdjustment(StockAdjustmentRequest request, Long staffUserId) {
        MedicineBatch batch = batchRepository.findById(request.getBatchId())
                .orElseThrow(() -> new ResourceNotFoundException("Batch not found: " + request.getBatchId()));

        int oldQty = batch.getAvailableQuantity();
        int newQty = oldQty;

        switch (request.getMovementType()) {
            case ADJUSTMENT, STOCK_RECEIVED, RETURN, RELEASED -> newQty = oldQty + request.getQuantity();
            case SALE, EXPIRED, RESERVED -> {
                if (oldQty < request.getQuantity()) {
                    throw new InventoryException("Insufficient stock in batch " + batch.getBatchNumber() +
                            ". Available: " + oldQty + ", requested deduction: " + request.getQuantity());
                }
                newQty = oldQty - request.getQuantity();
            }
        }

        batch.setAvailableQuantity(newQty);
        MedicineBatch updated = batchRepository.save(batch);

        StockMovement movement = StockMovement.builder()
                .batch(updated)
                .movementType(request.getMovementType())
                .quantity(request.getQuantity())
                .referenceType("MANUAL_ADJUSTMENT")
                .referenceId("ADJ-" + System.currentTimeMillis())
                .notes(request.getNotes())
                .createdByUserId(staffUserId)
                .build();
        movementRepository.save(movement);

        auditLogService.log(staffUserId, "STOCK_ADJUSTMENT", "MedicineBatch", updated.getId(),
                "Adjusted batch " + updated.getBatchNumber() + " (" + request.getMovementType() + "): " +
                        oldQty + " -> " + newQty);

        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public PageResponse<StockMovementResponse> getStockMovements(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<StockMovement> pageResult = movementRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<StockMovementResponse> content = pageResult.getContent().stream()
                .map(this::mapMovementToResponse)
                .collect(Collectors.toList());

        return PageResponse.<StockMovementResponse>builder()
                .content(content)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    private MedicineBatchResponse mapToResponse(MedicineBatch batch) {
        LocalDate today = LocalDate.now();
        boolean expired = batch.getExpiryDate().isBefore(today);
        boolean nearExpiry = !expired && batch.getExpiryDate().isBefore(today.plusDays(90));

        return MedicineBatchResponse.builder()
                .id(batch.getId())
                .medicineId(batch.getMedicine().getId())
                .medicineName(batch.getMedicine().getName())
                .batchNumber(batch.getBatchNumber())
                .manufacturingDate(batch.getManufacturingDate())
                .expiryDate(batch.getExpiryDate())
                .purchasePrice(batch.getPurchasePrice())
                .sellingPrice(batch.getSellingPrice())
                .quantityReceived(batch.getQuantityReceived())
                .availableQuantity(batch.getAvailableQuantity())
                .expired(expired)
                .nearExpiry(nearExpiry)
                .createdAt(batch.getCreatedAt())
                .build();
    }

    private StockMovementResponse mapMovementToResponse(StockMovement m) {
        return StockMovementResponse.builder()
                .id(m.getId())
                .batchId(m.getBatch().getId())
                .medicineName(m.getBatch().getMedicine().getName())
                .batchNumber(m.getBatch().getBatchNumber())
                .movementType(m.getMovementType())
                .quantity(m.getQuantity())
                .referenceType(m.getReferenceType())
                .referenceId(m.getReferenceId())
                .notes(m.getNotes())
                .createdByUserId(m.getCreatedByUserId())
                .createdAt(m.getCreatedAt())
                .build();
    }
}
