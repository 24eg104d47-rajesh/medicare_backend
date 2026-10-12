package com.medicareai.dto.inventory;

import com.medicareai.enums.StockMovementType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockMovementResponse {
    private Long id;
    private Long batchId;
    private String medicineName;
    private String batchNumber;
    private StockMovementType movementType;
    private Integer quantity;
    private String referenceType;
    private String referenceId;
    private String notes;
    private Long createdByUserId;
    private LocalDateTime createdAt;
}
