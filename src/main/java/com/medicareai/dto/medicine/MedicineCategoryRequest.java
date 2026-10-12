package com.medicareai.dto.medicine;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineCategoryRequest {
    @NotBlank(message = "Category name is required")
    private String name;

    private String description;

    @Builder.Default
    private boolean active = true;
}
