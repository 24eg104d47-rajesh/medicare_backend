package com.medicareai.dto.prescription;

import com.medicareai.enums.PrescriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionResponse {
    private Long id;
    private Long userId;
    private String customerName;
    private String customerEmail;
    private String originalFileName;
    private String contentType;
    private Long fileSize;
    private PrescriptionStatus status;
    private Long reviewerUserId;
    private String reviewerName;
    private String reviewNotes;
    private LocalDateTime uploadedAt;
    private LocalDateTime reviewedAt;
}
