package com.medicareai.repository;

import com.medicareai.entity.OrderItem;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrderId(Long orderId);

    @Query("SELECT oi.medicine.id, oi.medicineNameSnapshot, oi.brandSnapshot, " +
           "SUM(oi.quantity) as totalQty, SUM(oi.lineTotal) as totalRevenue " +
           "FROM OrderItem oi " +
           "GROUP BY oi.medicine.id, oi.medicineNameSnapshot, oi.brandSnapshot " +
           "ORDER BY totalQty DESC")
    List<Object[]> findTopSellingProducts(Pageable pageable);
}
