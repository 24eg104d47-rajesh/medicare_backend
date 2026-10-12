package com.medicareai.dto.order;

import com.medicareai.enums.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistoryResponse {
    private Long id;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private Long changedByUserId;
    private String notes;
    private LocalDateTime createdAt;
}
