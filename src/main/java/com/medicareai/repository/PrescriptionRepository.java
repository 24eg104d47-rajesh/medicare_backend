package com.medicareai.repository;

import com.medicareai.entity.Prescription;
import com.medicareai.enums.PrescriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrescriptionRepository extends JpaRepository<Prescription, Long> {
    List<Prescription> findByUserIdOrderByUploadedAtDesc(Long userId);
    Optional<Prescription> findByIdAndUserId(Long id, Long userId);
    Page<Prescription> findByStatusOrderByUploadedAtAsc(PrescriptionStatus status, Pageable pageable);
    long countByStatus(PrescriptionStatus status);
}
