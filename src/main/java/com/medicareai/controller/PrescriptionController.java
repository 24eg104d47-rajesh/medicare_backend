package com.medicareai.controller;

import com.medicareai.dto.common.ApiResponse;
import com.medicareai.dto.common.PageResponse;
import com.medicareai.dto.prescription.PrescriptionResponse;
import com.medicareai.dto.prescription.PrescriptionReviewRequest;
import com.medicareai.security.UserDetailsImpl;
import com.medicareai.service.PrescriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PrescriptionController {

    private final PrescriptionService prescriptionService;

    @PostMapping("/api/prescriptions/upload")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> uploadPrescription(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        PrescriptionResponse response = prescriptionService.uploadPrescription(userDetails.getId(), file);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Prescription uploaded successfully", response));
    }

    @GetMapping("/api/prescriptions/mine")
    public ResponseEntity<ApiResponse<List<PrescriptionResponse>>> getMyPrescriptions(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<PrescriptionResponse> list = prescriptionService.getMyPrescriptions(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/api/prescriptions/{id}")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> getPrescriptionById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        boolean isStaff = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PHARMACY_ADMIN"));
        PrescriptionResponse response = prescriptionService.getPrescriptionById(id, userDetails.getId(), isStaff);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/api/prescriptions/{id}/file")
    public ResponseEntity<Resource> downloadPrescriptionFile(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        boolean isStaff = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_PHARMACY_ADMIN"));
        Resource file = prescriptionService.getPrescriptionFile(id, userDetails.getId(), isStaff);

        PrescriptionResponse metadata = prescriptionService.getPrescriptionById(id, userDetails.getId(), isStaff);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(metadata.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + metadata.getOriginalFileName() + "\"")
                .body(file);
    }

    @GetMapping("/api/admin/prescriptions/pending")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACY_ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<PrescriptionResponse>>> getPendingPrescriptions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        PageResponse<PrescriptionResponse> result = prescriptionService.getPendingPrescriptions(page, size);
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PatchMapping("/api/admin/prescriptions/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACY_ADMIN')")
    public ResponseEntity<ApiResponse<PrescriptionResponse>> reviewPrescription(
            @PathVariable Long id,
            @Valid @RequestBody PrescriptionReviewRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        PrescriptionResponse reviewed = prescriptionService.reviewPrescription(id, request, userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("Prescription review recorded", reviewed));
    }
}
