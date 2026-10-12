package com.medicareai.dto.order;

import com.medicareai.dto.user.DeliveryAddressResponse;
import com.medicareai.enums.OrderStatus;
import com.medicareai.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private Long id;
    private String orderNumber;
    private Long userId;
    private String customerName;
    private String customerEmail;
    private OrderStatus orderStatus;
    private PaymentStatus paymentStatus;
    private BigDecimal subtotal;
    private BigDecimal deliveryFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String customerNotes;
    private String cancellationReason;
    private DeliveryAddressResponse deliveryAddress;
    @Builder.Default
    private List<OrderItemResponse> items = new ArrayList<>();
    @Builder.Default
    private List<Long> prescriptionIds = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
