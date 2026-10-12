package com.medicareai.dto.inventory;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineBatchResponse {
    private Long id;
    private Long medicineId;
    private String medicineName;
    private String batchNumber;
    private LocalDate manufacturingDate;
    private LocalDate expiryDate;
    private BigDecimal purchasePrice;
    private BigDecimal sellingPrice;
    private int quantityReceived;
    private int availableQuantity;
    private boolean expired;
    private boolean nearExpiry;
    private LocalDateTime createdAt;
}
