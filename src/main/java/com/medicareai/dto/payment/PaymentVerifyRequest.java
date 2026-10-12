package com.medicareai.dto.payment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentVerifyRequest {
    @NotNull(message = "Order ID is required")
    private Long orderId;

    @NotBlank(message = "Provider payment ID is required")
    private String providerPaymentId;

    private String providerOrderId;

    private String providerSignature;
}
