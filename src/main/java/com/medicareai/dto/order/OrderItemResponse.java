package com.medicareai.dto.order;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItemResponse {
    private Long id;
    private Long medicineId;
    private String medicineName;
    private String brand;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal lineTotal;
    private Long batchId;
    private String batchNumber;
}
