package com.medicareai.dto.cart;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {
    private Long id;
    private Long medicineId;
    private String medicineName;
    private String brand;
    private String dosageForm;
    private String strength;
    private String productImageUrl;
    private boolean requiresPrescription;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal lineTotal;
    private int availableStock;
}
