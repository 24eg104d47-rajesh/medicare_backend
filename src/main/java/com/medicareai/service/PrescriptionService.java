package com.medicareai.service;

import com.medicareai.dto.common.PageResponse;
import com.medicareai.dto.prescription.PrescriptionResponse;
import com.medicareai.dto.prescription.PrescriptionReviewRequest;
import com.medicareai.entity.Prescription;
import com.medicareai.entity.User;
import com.medicareai.enums.PrescriptionStatus;
import com.medicareai.exception.BadRequestException;
import com.medicareai.exception.ResourceNotFoundException;
import com.medicareai.exception.UnauthorizedException;
import com.medicareai.repository.PrescriptionRepository;
import com.medicareai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PrescriptionService {

    private final PrescriptionRepository prescriptionRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Value("${app.storage.upload-dir:uploads/prescriptions}")
    private String uploadDir;

    private static final List<String> ALLOWED_EXTENSIONS = Arrays.asList("pdf", "jpg", "jpeg", "png", "webp");
    private static final List<String> ALLOWED_MIME_TYPES = Arrays.asList(
            "application/pdf", "image/jpeg", "image/png", "image/webp"
    );

    @Transactional
    public PrescriptionResponse uploadPrescription(Long userId, MultipartFile file) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (file.isEmpty()) {
            throw new BadRequestException("Prescription file cannot be empty");
        }

        if (file.getSize() > 10 * 1024 * 1024) {
            throw new BadRequestException("File size exceeds 10MB limit");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new BadRequestException("Invalid file name");
        }

        String extension = "";
        int dotIndex = originalFilename.lastIndexOf('.');
        if (dotIndex > 0) {
            extension = originalFilename.substring(dotIndex + 1).toLowerCase();
        }

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BadRequestException("Invalid file extension. Allowed: PDF, JPG, PNG, WEBP");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException("Invalid file format. Allowed: PDF, JPG, PNG, WEBP");
        }

        // Generate unique storage key
        String storageKey = UUID.randomUUID() + "." + extension;

        try {
            Path targetPath = Paths.get(uploadDir).toAbsolutePath().normalize();
            Files.createDirectories(targetPath);
            Path filePath = targetPath.resolve(storageKey);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Could not store prescription file: " + e.getMessage(), e);
        }

        Prescription prescription = Prescription.builder()
                .user(user)
                .fileStorageKey(storageKey)
                .originalFileName(originalFilename)
                .contentType(contentType)
                .fileSize(file.getSize())
                .status(PrescriptionStatus.UPLOADED)
                .build();

        Prescription saved = prescriptionRepository.save(prescription);

        notificationService.createNotification(userId, "Prescription Uploaded",
                "Your prescription '" + originalFilename + "' was uploaded and is queued for verification.",
                "PRESCRIPTION");

        auditLogService.log(userId, "PRESCRIPTION_UPLOADED", "Prescription", saved.getId(),
                "Prescription uploaded: " + originalFilename);

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<PrescriptionResponse> getMyPrescriptions(Long userId) {
        return prescriptionRepository.findByUserIdOrderByUploadedAtDesc(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PrescriptionResponse getPrescriptionById(Long prescriptionId, Long requestingUserId, boolean isStaff) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + prescriptionId));

        if (!isStaff && !prescription.getUser().getId().equals(requestingUserId)) {
            throw new UnauthorizedException("Access denied to this prescription");
        }

        return mapToResponse(prescription);
    }

    @Transactional(readOnly = true)
    public PageResponse<PrescriptionResponse> getPendingPrescriptions(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Prescription> pageResult = prescriptionRepository.findByStatusOrderByUploadedAtAsc(
                PrescriptionStatus.UPLOADED, pageable);

        List<PrescriptionResponse> content = pageResult.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<PrescriptionResponse>builder()
                .content(content)
                .pageNumber(pageResult.getNumber())
                .pageSize(pageResult.getSize())
                .totalElements(pageResult.getTotalElements())
                .totalPages(pageResult.getTotalPages())
                .last(pageResult.isLast())
                .build();
    }

    @Transactional
    public PrescriptionResponse reviewPrescription(Long prescriptionId, PrescriptionReviewRequest request, Long reviewerUserId) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + prescriptionId));

        User reviewer = userRepository.findById(reviewerUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Reviewer not found: " + reviewerUserId));

        if (request.getStatus() != PrescriptionStatus.APPROVED && request.getStatus() != PrescriptionStatus.REJECTED) {
            throw new BadRequestException("Review status must be APPROVED or REJECTED");
        }

        prescription.setStatus(request.getStatus());
        prescription.setReviewerUser(reviewer);
        prescription.setReviewNotes(request.getReviewNotes());
        prescription.setReviewedAt(LocalDateTime.now());

        Prescription updated = prescriptionRepository.save(prescription);

        String title = request.getStatus() == PrescriptionStatus.APPROVED
                ? "Prescription Approved"
                : "Prescription Update";
        String msg = request.getStatus() == PrescriptionStatus.APPROVED
                ? "Your prescription '" + prescription.getOriginalFileName() + "' has been verified and approved by a pharmacist."
                : "Your prescription '" + prescription.getOriginalFileName() + "' was not approved. Reason: " +
                  (request.getReviewNotes() != null ? request.getReviewNotes() : "Incomplete or unclear image");

        notificationService.createNotification(prescription.getUser().getId(), title, msg, "PRESCRIPTION");

        auditLogService.log(reviewerUserId, "PRESCRIPTION_REVIEWED", "Prescription", updated.getId(),
                "Prescription " + updated.getId() + " reviewed as " + request.getStatus());

        return mapToResponse(updated);
    }

    @Transactional(readOnly = true)
    public Resource getPrescriptionFile(Long prescriptionId, Long requestingUserId, boolean isStaff) {
        Prescription prescription = prescriptionRepository.findById(prescriptionId)
                .orElseThrow(() -> new ResourceNotFoundException("Prescription not found: " + prescriptionId));

        if (!isStaff && !prescription.getUser().getId().equals(requestingUserId)) {
            throw new UnauthorizedException("Access denied to this prescription file");
        }

        try {
            Path filePath = Paths.get(uploadDir).toAbsolutePath().normalize().resolve(prescription.getFileStorageKey());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found on server");
            }
        } catch (MalformedURLException e) {
            throw new ResourceNotFoundException("File path error: " + e.getMessage());
        }
    }

    private PrescriptionResponse mapToResponse(Prescription p) {
        return PrescriptionResponse.builder()
                .id(p.getId())
                .userId(p.getUser().getId())
                .customerName(p.getUser().getFullName())
                .customerEmail(p.getUser().getEmail())
                .originalFileName(p.getOriginalFileName())
                .contentType(p.getContentType())
                .fileSize(p.getFileSize())
                .status(p.getStatus())
                .reviewerUserId(p.getReviewerUser() != null ? p.getReviewerUser().getId() : null)
                .reviewerName(p.getReviewerUser() != null ? p.getReviewerUser().getFullName() : null)
                .reviewNotes(p.getReviewNotes())
                .uploadedAt(p.getUploadedAt())
                .reviewedAt(p.getReviewedAt())
                .build();
    }
}
