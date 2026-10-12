package com.medicareai.repository;

import com.medicareai.entity.StockMovement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface StockMovementRepository extends JpaRepository<StockMovement, Long> {
    List<StockMovement> findByBatchIdOrderByCreatedAtDesc(Long batchId);
    Page<StockMovement> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
