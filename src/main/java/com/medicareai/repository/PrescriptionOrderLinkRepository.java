package com.medicareai.repository;

import com.medicareai.entity.PrescriptionOrderLink;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PrescriptionOrderLinkRepository extends JpaRepository<PrescriptionOrderLink, Long> {
    List<PrescriptionOrderLink> findByOrderId(Long orderId);
    boolean existsByOrderIdAndPrescriptionId(Long orderId, Long prescriptionId);
}
