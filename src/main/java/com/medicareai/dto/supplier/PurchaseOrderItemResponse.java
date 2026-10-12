package com.medicareai.dto.supplier;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrderItemResponse {
    private Long id;
    private Long medicineId;
    private String medicineName;
    private String brand;
    private int quantity;
    private BigDecimal unitCost;
    private BigDecimal lineTotal;
}
