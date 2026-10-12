package com.medicareai.dto.medicine;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineRequest {
    @NotNull(message = "Category ID is required")
    private Long categoryId;

    @NotBlank(message = "Medicine name is required")
    private String name;

    @NotBlank(message = "Brand is required")
    private String brand;

    @NotBlank(message = "Generic name is required")
    private String genericName;

    @NotBlank(message = "Manufacturer is required")
    private String manufacturer;

    private String description;

    @NotBlank(message = "Dosage form is required")
    private String dosageForm;

    @NotBlank(message = "Strength is required")
    private String strength;

    @NotBlank(message = "Pack size is required")
    private String packSize;

    private String productImageUrl;

    @Builder.Default
    private boolean requiresPrescription = false;

    @Builder.Default
    private boolean active = true;
}
