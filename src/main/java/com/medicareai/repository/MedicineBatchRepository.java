package com.medicareai.repository;

import com.medicareai.entity.MedicineBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface MedicineBatchRepository extends JpaRepository<MedicineBatch, Long> {

    List<MedicineBatch> findByMedicineId(Long medicineId);

    Optional<MedicineBatch> findByMedicineIdAndBatchNumber(Long medicineId, String batchNumber);

    // FEFO: First-Expiry-First-Out for eligible non-expired batches with available stock
    @Query("SELECT b FROM MedicineBatch b WHERE b.medicine.id = :medicineId " +
           "AND b.expiryDate > :currentDate AND b.availableQuantity > 0 " +
           "ORDER BY b.expiryDate ASC")
    List<MedicineBatch> findEligibleBatchesForSale(@Param("medicineId") Long medicineId,
                                                  @Param("currentDate") LocalDate currentDate);

    // Sum of sellable quantity for a medicine (non-expired)
    @Query("SELECT COALESCE(SUM(b.availableQuantity), 0) FROM MedicineBatch b " +
           "WHERE b.medicine.id = :medicineId AND b.expiryDate > :currentDate AND b.availableQuantity > 0")
    int getAvailableStockForMedicine(@Param("medicineId") Long medicineId,
                                    @Param("currentDate") LocalDate currentDate);

    // Low stock batches
    @Query("SELECT b FROM MedicineBatch b WHERE b.availableQuantity <= :threshold AND b.expiryDate > :currentDate")
    List<MedicineBatch> findLowStockBatches(@Param("threshold") int threshold,
                                           @Param("currentDate") LocalDate currentDate);

    // Batches expiring within the next X days
    @Query("SELECT b FROM MedicineBatch b WHERE b.expiryDate >= :currentDate AND b.expiryDate <= :targetDate AND b.availableQuantity > 0")
    List<MedicineBatch> findExpiringBatches(@Param("currentDate") LocalDate currentDate,
                                           @Param("targetDate") LocalDate targetDate);

    // Expired batches
    List<MedicineBatch> findByExpiryDateLessThanEqual(LocalDate currentDate);
}
