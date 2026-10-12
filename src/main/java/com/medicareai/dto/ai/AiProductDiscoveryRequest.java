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
public class AiProductDiscoveryRequest {
    @NotBlank(message = "Search query or wellness need description is required")
    private String description;
}
