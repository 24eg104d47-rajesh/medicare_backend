package com.medicareai.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiMedicineInfoRequest {
    @NotBlank(message = "Query or question is required")
    private String query;

    private Long medicineId;
}
