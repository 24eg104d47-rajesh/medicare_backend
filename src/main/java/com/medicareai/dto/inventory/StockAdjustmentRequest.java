package com.medicareai.dto.inventory;

import com.medicareai.enums.StockMovementType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockAdjustmentRequest {
    @NotNull(message = "Batch ID is required")
    private Long batchId;

    @NotNull(message = "Quantity is required")
    private Integer quantity;

    @NotNull(message = "Movement type is required")
    private StockMovementType movementType;

    private String notes;
}
