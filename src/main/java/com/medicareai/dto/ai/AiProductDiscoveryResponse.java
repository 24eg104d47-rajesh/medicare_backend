package com.medicareai.dto.ai;

import com.medicareai.dto.medicine.MedicineResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiProductDiscoveryResponse {
    private String advice;
    @Builder.Default
    private List<MedicineResponse> matchedProducts = new ArrayList<>();
    @Builder.Default
    private String disclaimer = "Informational guidance only based on non-clinical store items. For severe or persistent symptoms, seek medical evaluation immediately.";
}
