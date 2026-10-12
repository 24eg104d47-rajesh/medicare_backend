package com.medicareai.dto.medicine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineResponse {
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String name;
    private String brand;
    private String genericName;
    private String manufacturer;
    private String description;
    private String dosageForm;
    private String strength;
    private String packSize;
    private String productImageUrl;
    private boolean requiresPrescription;
    private boolean active;
    private int availableStock;
    private BigDecimal price;
    private LocalDateTime createdAt;

    public int getTotalStock() {
        return availableStock;
    }

    public boolean isPrescriptionRequired() {
        return requiresPrescription;
    }
}
