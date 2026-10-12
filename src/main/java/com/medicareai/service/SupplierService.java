package com.medicareai.service;

import com.medicareai.dto.common.PageResponse;
import com.medicareai.dto.supplier.*;
import com.medicareai.entity.*;
import com.medicareai.enums.PurchaseOrderStatus;
import com.medicareai.enums.StockMovementType;
import com.medicareai.exception.BadRequestException;
import com.medicareai.exception.ResourceNotFoundException;
import com.medicareai.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SupplierService {

    private final SupplierRepository supplierRepository;
    private final PurchaseOrderRepository purchaseOrderRepository;
    private final PurchaseOrderItemRepository purchaseOrderItemRepository;
    private final MedicineRepository medicineRepository;
    private final MedicineBatchRepository batchRepository;
    private final StockMovementRepository movementRepository;
    private final AuditLogService auditLogService;

    // SUPPLIER CRUD
    @Transactional(readOnly = true)
    public List<SupplierResponse> getAllSuppliers(boolean activeOnly) {
        List<Supplier> suppliers = activeOnly ? supplierRepository.findByActiveTrueOrderByNameAsc() : supplierRepository.findAll();
        return suppliers.stream().map(this::mapSupplierToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplierById(Long id) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));
        return mapSupplierToResponse(supplier);
    }

    @Transactional
    public SupplierResponse createSupplier(SupplierRequest request, Long staffUserId) {
        Supplier supplier = Supplier.builder()
                .name(request.getName().trim())
                .contactPerson(request.getContactPerson())
                .email(request.getEmail())
                .phone(request.getPhone())
                .address(request.getAddress())
                .active(request.isActive())
                .build();
        Supplier saved = supplierRepository.save(supplier);

        auditLogService.log(staffUserId, "SUPPLIER_CREATED", "Supplier", saved.getId(),
                "Created supplier: " + saved.getName());

        return mapSupplierToResponse(saved);
    }

    @Transactional
    public SupplierResponse updateSupplier(Long id, SupplierRequest request, Long staffUserId) {
        Supplier supplier = supplierRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + id));

        supplier.setName(request.getName().trim());
        supplier.setContactPerson(request.getContactPerson());
        supplier.setEmail(request.getEmail());
        supplier.setPhone(request.getPhone());
        supplier.setAddress(request.getAddress());
        supplier.setActive(request.isActive());

        Supplier updated = supplierRepository.save(supplier);

        auditLogService.log(staffUserId, "SUPPLIER_UPDATED", "Supplier", updated.getId(),
                "Updated supplier: " + updated.getName());

        return mapSupplierToResponse(updated);
    }

    // PURCHASE ORDERS
    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderResponse> getAllPurchaseOrders(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<PurchaseOrder> pageResult = purchaseOrderRepository.findAllByOrderByCreatedAtDesc(pageable);

        List<PurchaseOrderResponse> content = pageResult.getContent().stream()
                .map(this::enrichPurchaseOrderResponse)
                .collect(Collectors.toList());

        return PageResponse.<PurchaseOrderResponse>builder()
                .content(content)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Transactional(readOnly = true)
    public PurchaseOrderResponse getPurchaseOrderById(Long id) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found: " + id));
        return enrichPurchaseOrderResponse(po);
    }

    @Transactional
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request, Long staffUserId) {
        Supplier supplier = supplierRepository.findById(request.getSupplierId())
                .orElseThrow(() -> new ResourceNotFoundException("Supplier not found: " + request.getSupplierId()));

        String poNumber = "PO-" + System.currentTimeMillis() + "-" + (100 + new Random().nextInt(900));

        PurchaseOrder po = PurchaseOrder.builder()
                .purchaseOrderNumber(poNumber)
                .supplier(supplier)
                .createdByUserId(staffUserId)
                .status(PurchaseOrderStatus.DRAFT)
                .notes(request.getNotes())
                .totalAmount(BigDecimal.ZERO)
                .build();

        PurchaseOrder savedPo = purchaseOrderRepository.save(po);

        BigDecimal total = BigDecimal.ZERO;
        List<PurchaseOrderItem> items = new ArrayList<>();

        for (PurchaseOrderItemRequest itemReq : request.getItems()) {
            Medicine med = medicineRepository.findById(itemReq.getMedicineId())
                    .orElseThrow(() -> new ResourceNotFoundException("Medicine not found: " + itemReq.getMedicineId()));

            BigDecimal lineTotal = itemReq.getUnitCost().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            total = total.add(lineTotal);

            PurchaseOrderItem poi = PurchaseOrderItem.builder()
                    .purchaseOrder(savedPo)
                    .medicine(med)
                    .quantity(itemReq.getQuantity())
                    .unitCost(itemReq.getUnitCost())
                    .lineTotal(lineTotal)
                    .build();

            items.add(purchaseOrderItemRepository.save(poi));
        }

        savedPo.setTotalAmount(total);
        purchaseOrderRepository.save(savedPo);

        auditLogService.log(staffUserId, "PURCHASE_ORDER_CREATED", "PurchaseOrder", savedPo.getId(),
                "Created PO #" + savedPo.getPurchaseOrderNumber() + " for supplier " + supplier.getName() + " with total ₹" + total);

        return enrichPurchaseOrderResponse(savedPo);
    }

    @Transactional
    public PurchaseOrderResponse updatePurchaseOrderStatus(Long id, PurchaseOrderStatus status, Long staffUserId) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found: " + id));

        if (po.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new BadRequestException("Cannot modify a purchase order that is already received");
        }

        po.setStatus(status);
        PurchaseOrder updated = purchaseOrderRepository.save(po);

        auditLogService.log(staffUserId, "PURCHASE_ORDER_STATUS_UPDATED", "PurchaseOrder", updated.getId(),
                "Updated PO #" + updated.getPurchaseOrderNumber() + " status to " + status);

        return enrichPurchaseOrderResponse(updated);
    }

    @Transactional
    public PurchaseOrderResponse receivePurchaseOrder(Long id, ReceivePurchaseOrderRequest request, Long staffUserId) {
        PurchaseOrder po = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Purchase order not found: " + id));

        if (po.getStatus() == PurchaseOrderStatus.RECEIVED) {
            throw new BadRequestException("Purchase order has already been received");
        }

        if (po.getStatus() == PurchaseOrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot receive a cancelled purchase order");
        }

        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderId(po.getId());
        Map<Long, ReceiveItemBatchRequest> batchMap = request.getBatchDetails().stream()
                .collect(Collectors.toMap(ReceiveItemBatchRequest::getPurchaseOrderItemId, b -> b));

        for (PurchaseOrderItem item : items) {
            ReceiveItemBatchRequest batchInfo = batchMap.get(item.getId());
            if (batchInfo == null) {
                throw new BadRequestException("Missing batch details for PO item ID: " + item.getId() +
                        " (" + item.getMedicine().getName() + ")");
            }

            if (batchInfo.getExpiryDate().isBefore(batchInfo.getManufacturingDate())) {
                throw new BadRequestException("Expiry date cannot be before manufacturing date for batch: " + batchInfo.getBatchNumber());
            }

            if (batchInfo.getExpiryDate().isBefore(LocalDate.now())) {
                throw new BadRequestException("Cannot receive expired stock for batch: " + batchInfo.getBatchNumber());
            }

            // Create new MedicineBatch
            MedicineBatch batch = MedicineBatch.builder()
                    .medicine(item.getMedicine())
                    .batchNumber(batchInfo.getBatchNumber().trim())
                    .manufacturingDate(batchInfo.getManufacturingDate())
                    .expiryDate(batchInfo.getExpiryDate())
                    .purchasePrice(item.getUnitCost())
                    .sellingPrice(batchInfo.getSellingPrice())
                    .quantityReceived(item.getQuantity())
                    .availableQuantity(item.getQuantity())
                    .build();

            MedicineBatch savedBatch = batchRepository.save(batch);

            // Record stock movement (STOCK_RECEIVED)
            StockMovement movement = StockMovement.builder()
                    .batch(savedBatch)
                    .movementType(StockMovementType.STOCK_RECEIVED)
                    .quantity(item.getQuantity())
                    .referenceType("PURCHASE_ORDER")
                    .referenceId(po.getPurchaseOrderNumber())
                    .notes("Stock received from PO #" + po.getPurchaseOrderNumber() + " (Supplier: " + po.getSupplier().getName() + ")")
                    .createdByUserId(staffUserId)
                    .build();
            movementRepository.save(movement);
        }

        po.setStatus(PurchaseOrderStatus.RECEIVED);
        PurchaseOrder updatedPo = purchaseOrderRepository.save(po);

        auditLogService.log(staffUserId, "PURCHASE_ORDER_RECEIVED", "PurchaseOrder", updatedPo.getId(),
                "Stock recorded and received for PO #" + updatedPo.getPurchaseOrderNumber());

        return enrichPurchaseOrderResponse(updatedPo);
    }

    private SupplierResponse mapSupplierToResponse(Supplier s) {
        return SupplierResponse.builder()
                .id(s.getId())
                .name(s.getName())
                .contactPerson(s.getContactPerson())
                .email(s.getEmail())
                .phone(s.getPhone())
                .address(s.getAddress())
                .active(s.isActive())
                .createdAt(s.getCreatedAt())
                .build();
    }

    private PurchaseOrderResponse enrichPurchaseOrderResponse(PurchaseOrder po) {
        List<PurchaseOrderItem> items = purchaseOrderItemRepository.findByPurchaseOrderId(po.getId());
        List<PurchaseOrderItemResponse> itemDtos = items.stream().map(i -> PurchaseOrderItemResponse.builder()
                .id(i.getId())
                .medicineId(i.getMedicine().getId())
                .medicineName(i.getMedicine().getName())
                .brand(i.getMedicine().getBrand())
                .quantity(i.getQuantity())
                .unitCost(i.getUnitCost())
                .lineTotal(i.getLineTotal())
                .build()).collect(Collectors.toList());

        return PurchaseOrderResponse.builder()
                .id(po.getId())
                .purchaseOrderNumber(po.getPurchaseOrderNumber())
                .supplierId(po.getSupplier().getId())
                .supplierName(po.getSupplier().getName())
                .createdByUserId(po.getCreatedByUserId())
                .status(po.getStatus())
                .totalAmount(po.getTotalAmount())
                .notes(po.getNotes())
                .items(itemDtos)
                .createdAt(po.getCreatedAt())
                .updatedAt(po.getUpdatedAt())
                .build();
    }
}
