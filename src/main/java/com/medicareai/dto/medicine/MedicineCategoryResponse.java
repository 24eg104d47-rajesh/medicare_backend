package com.medicareai.dto.medicine;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicineCategoryResponse {
    private Long id;
    private String name;
    private String description;
    private boolean active;
    private long medicineCount;
    private LocalDateTime createdAt;
}
