package com.medicareai.dto.supplier;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItemRequest {
    @NotNull(message = "Medicine ID is required")
    private Long medicineId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity;

    @NotNull(message = "Unit cost is required")
    @DecimalMin(value = "0.01", message = "Unit cost must be positive")
    private BigDecimal unitCost;
}
