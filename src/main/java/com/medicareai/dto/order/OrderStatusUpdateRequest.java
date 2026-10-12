package com.medicareai.dto.order;

import com.medicareai.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusUpdateRequest {
    @NotNull(message = "New order status is required")
    private OrderStatus newStatus;

    private String notes;
}
