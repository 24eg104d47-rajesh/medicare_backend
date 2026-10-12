package com.medicareai.repository;

import com.medicareai.entity.DeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, Long> {
    List<DeliveryAddress> findByUserIdOrderByDefaultAddressDescCreatedAtDesc(Long userId);
    Optional<DeliveryAddress> findByIdAndUserId(Long id, Long userId);
    Optional<DeliveryAddress> findByUserIdAndDefaultAddressTrue(Long userId);
}
