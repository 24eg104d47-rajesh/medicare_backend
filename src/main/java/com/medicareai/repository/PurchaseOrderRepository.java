package com.medicareai.repository;

import com.medicareai.entity.PurchaseOrder;
import com.medicareai.enums.PurchaseOrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Long> {
    Optional<PurchaseOrder> findByPurchaseOrderNumber(String purchaseOrderNumber);
    List<PurchaseOrder> findBySupplierId(Long supplierId);
    Page<PurchaseOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);
    long countByStatus(PurchaseOrderStatus status);
}
