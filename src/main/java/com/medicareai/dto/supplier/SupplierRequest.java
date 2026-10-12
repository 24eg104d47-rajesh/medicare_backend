package com.medicareai.dto.supplier;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierRequest {
    @NotBlank(message = "Supplier name is required")
    private String name;

    private String contactPerson;
    private String email;
    private String phone;
    private String address;

    @Builder.Default
    private boolean active = true;
}
