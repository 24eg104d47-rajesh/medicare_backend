package com.medicareai.dto.payment;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiateRequest {
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @Builder.Default
    private String provider = "RAZORPAY_SANDBOX";
}
