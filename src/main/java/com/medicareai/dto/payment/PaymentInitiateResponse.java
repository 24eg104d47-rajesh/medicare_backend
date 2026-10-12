package com.medicareai.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentInitiateResponse {
    private Long paymentId;
    private Long orderId;
    private String orderNumber;
    private String provider;
    private String providerOrderId;
    private BigDecimal amount;
    private String currency;
    private String keyId;
    private String customerName;
    private String customerEmail;
}
