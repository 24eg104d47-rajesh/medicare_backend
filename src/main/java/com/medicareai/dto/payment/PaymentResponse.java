package com.medicareai.dto.payment;

import com.medicareai.enums.PaymentTransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {
    private Long id;
    private Long orderId;
    private String provider;
    private String providerOrderId;
    private String providerPaymentId;
    private BigDecimal amount;
    private String currency;
    private PaymentTransactionStatus status;
    private LocalDateTime createdAt;
}
