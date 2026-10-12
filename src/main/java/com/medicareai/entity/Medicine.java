package com.medicareai.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "medicines", indexes = {
    @Index(name = "idx_med_name", columnList = "name"),
    @Index(name = "idx_med_brand", columnList = "brand"),
    @Index(name = "idx_med_generic", columnList = "generic_name"),
    @Index(name = "idx_med_active", columnList = "active"),
    @Index(name = "idx_med_category", columnList = "category_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medicine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private MedicineCategory category;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false, length = 150)
    private String brand;

    @Column(name = "generic_name", nullable = false, length = 200)
    private String genericName;

    @Column(nullable = false, length = 150)
    private String manufacturer;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "dosage_form", nullable = false, length = 50)
    private String dosageForm;

    @Column(nullable = false, length = 50)
    private String strength;

    @Column(name = "pack_size", nullable = false, length = 100)
    private String packSize;

    @Column(name = "product_image_url", length = 500)
    private String productImageUrl;

    @Column(name = "requires_prescription", nullable = false)
    @Builder.Default
    private boolean requiresPrescription = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
