package com.medicareai.repository;

import com.medicareai.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByOrderId(Long orderId);
    Optional<Payment> findByProviderPaymentId(String providerPaymentId);
    Optional<Payment> findByProviderOrderId(String providerOrderId);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
}
