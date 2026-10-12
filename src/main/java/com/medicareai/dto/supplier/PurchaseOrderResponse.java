package com.medicareai.dto.supplier;

import com.medicareai.enums.PurchaseOrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderResponse {
    private Long id;
    private String purchaseOrderNumber;
    private Long supplierId;
    private String supplierName;
    private Long createdByUserId;
    private PurchaseOrderStatus status;
    private BigDecimal totalAmount;
    private String notes;
    @Builder.Default
    private List<PurchaseOrderItemResponse> items = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
