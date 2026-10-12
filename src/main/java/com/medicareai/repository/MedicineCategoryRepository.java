package com.medicareai.repository;

import com.medicareai.entity.MedicineCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineCategoryRepository extends JpaRepository<MedicineCategory, Long> {
    Optional<MedicineCategory> findByName(String name);
    Optional<MedicineCategory> findByNameIgnoreCase(String name);
    boolean existsByName(String name);
    List<MedicineCategory> findByActiveTrueOrderByNameAsc();
}
