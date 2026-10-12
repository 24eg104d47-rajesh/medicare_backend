package com.medicareai.dto.ai;

import com.medicareai.dto.medicine.MedicineResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiMedicineInfoResponse {
    private String answer;
    private MedicineResponse medicineDetails;
    @Builder.Default
    private String safetyDisclaimer = "General information only. Always consult a physician or pharmacist for clinical questions.";
}
