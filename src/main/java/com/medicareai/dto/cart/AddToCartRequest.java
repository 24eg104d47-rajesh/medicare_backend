package com.medicareai.dto.cart;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartRequest {
    @NotNull(message = "Medicine ID is required")
    private Long medicineId;

    @Min(value = 1, message = "Quantity must be at least 1")
    @Builder.Default
    private int quantity = 1;
}
