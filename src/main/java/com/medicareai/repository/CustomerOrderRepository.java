package com.medicareai.repository;

import com.medicareai.entity.CustomerOrder;
import com.medicareai.enums.OrderStatus;
import com.medicareai.enums.PaymentStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerOrderRepository extends JpaRepository<CustomerOrder, Long> {

    List<CustomerOrder> findByUserIdOrderByCreatedAtDesc(Long userId);

    Optional<CustomerOrder> findByOrderNumber(String orderNumber);

    Optional<CustomerOrder> findByIdAndUserId(Long id, Long userId);

    boolean existsByDeliveryAddressId(Long deliveryAddressId);

    Page<CustomerOrder> findAllByOrderByCreatedAtDesc(Pageable pageable);

    long countByOrderStatus(OrderStatus status);

    long countByPaymentStatus(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM CustomerOrder o WHERE o.paymentStatus = 'PAID'")
    BigDecimal calculateTotalPaidSales();

    @Query("SELECT o FROM CustomerOrder o ORDER BY o.createdAt DESC")
    List<CustomerOrder> findRecentOrders(Pageable pageable);
}
