package com.medicareai.dto.order;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CheckoutRequest {
    @NotNull(message = "Delivery address ID is required")
    private Long deliveryAddressId;

    private List<Long> prescriptionIds;

    private String customerNotes;

    @Builder.Default
    private String paymentProvider = "RAZORPAY_SANDBOX";
}
