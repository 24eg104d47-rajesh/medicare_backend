package com.medicareai.repository;

import com.medicareai.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCartId(Long cartId);
    Optional<CartItem> findByCartIdAndMedicineId(Long cartId, Long medicineId);
    Optional<CartItem> findByIdAndCartUserId(Long id, Long userId);
    void deleteByCartId(Long cartId);
}
