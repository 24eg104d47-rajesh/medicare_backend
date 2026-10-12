package com.medicareai.dto.prescription;

import com.medicareai.enums.PrescriptionStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionReviewRequest {
    @NotNull(message = "Review status is required (APPROVED or REJECTED)")
    private PrescriptionStatus status;

    private String reviewNotes;
}
