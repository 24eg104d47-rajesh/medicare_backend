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
public class AiChatResponse {
    private Long conversationId;
    private String message;
    private String intent;
    @Builder.Default
    private List<MedicineResponse> recommendedMedicines = new ArrayList<>();
    @Builder.Default
    private String disclaimer = "Medical Disclaimer: MediCare AI provides general informational assistance based on catalogue inventory. It is not a substitute for professional medical advice, diagnosis, or prescription from a licensed healthcare provider.";
}
