package com.medicareai.service;

import com.medicareai.dto.payment.PaymentInitiateRequest;
import com.medicareai.dto.payment.PaymentInitiateResponse;
import com.medicareai.dto.payment.PaymentResponse;
import com.medicareai.dto.payment.PaymentVerifyRequest;
import com.medicareai.entity.CustomerOrder;
import com.medicareai.entity.OrderStatusHistory;
import com.medicareai.entity.Payment;
import com.medicareai.enums.OrderStatus;
import com.medicareai.enums.PaymentStatus;
import com.medicareai.enums.PaymentTransactionStatus;
import com.medicareai.exception.BadRequestException;
import com.medicareai.exception.ResourceNotFoundException;
import com.medicareai.exception.UnauthorizedException;
import com.medicareai.repository.CustomerOrderRepository;
import com.medicareai.repository.OrderStatusHistoryRepository;
import com.medicareai.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final CustomerOrderRepository orderRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final NotificationService notificationService;
    private final AuditLogService auditLogService;

    @Value("${app.payment.provider:RAZORPAY_SANDBOX}")
    private String paymentProvider;

    @Value("${app.payment.key-id:rzp_test_medicareai123}")
    private String paymentKeyId;

    @Transactional
    public PaymentInitiateResponse initiatePayment(Long userId, PaymentInitiateRequest request) {
        CustomerOrder order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));

        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied to this order");
        }

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Order is already paid");
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new BadRequestException("Cannot pay for a cancelled order");
        }

        String providerOrderId = "order_mock_" + System.currentTimeMillis();
        String idempotencyKey = UUID.randomUUID().toString();

        Payment payment = Payment.builder()
                .order(order)
                .provider(paymentProvider)
                .providerOrderId(providerOrderId)
                .amount(order.getTotalAmount())
                .currency("INR")
                .status(PaymentTransactionStatus.PENDING)
                .idempotencyKey(idempotencyKey)
                .build();

        Payment saved = paymentRepository.save(payment);

        return PaymentInitiateResponse.builder()
                .paymentId(saved.getId())
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .provider(paymentProvider)
                .providerOrderId(providerOrderId)
                .amount(order.getTotalAmount())
                .currency("INR")
                .keyId(paymentKeyId)
                .customerName(order.getUser().getFullName())
                .customerEmail(order.getUser().getEmail())
                .build();
    }

    @Transactional
    public PaymentResponse verifyPayment(Long userId, PaymentVerifyRequest request) {
        CustomerOrder order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + request.getOrderId()));

        if (!order.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("Access denied to this order");
        }

        // Idempotency check: if order is already paid, return existing success payment
        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            return paymentRepository.findByOrderId(order.getId()).stream()
                    .filter(p -> p.getStatus() == PaymentTransactionStatus.SUCCESS)
                    .findFirst()
                    .map(this::mapToResponse)
                    .orElseThrow(() -> new BadRequestException("Order is already marked paid"));
        }

        // Server-side validation of provider payment ID
        if (request.getProviderPaymentId() == null || request.getProviderPaymentId().isBlank()) {
            throw new BadRequestException("Invalid payment verification details: Missing payment reference");
        }

        // Check if this providerPaymentId has already been used for another payment (idempotency & replay protection)
        if (paymentRepository.findByProviderPaymentId(request.getProviderPaymentId()).isPresent()) {
            throw new BadRequestException("Payment reference has already been processed");
        }

        // In sandbox mode: verify payment ID format (e.g. pay_mock_... or real Razorpay pay_...)
        Payment payment = Payment.builder()
                .order(order)
                .provider(paymentProvider)
                .providerOrderId(request.getProviderOrderId())
                .providerPaymentId(request.getProviderPaymentId())
                .amount(order.getTotalAmount())
                .currency("INR")
                .status(PaymentTransactionStatus.SUCCESS)
                .idempotencyKey(UUID.randomUUID().toString())
                .build();
        Payment saved = paymentRepository.save(payment);

        // Update Order Status
        OrderStatus prevStatus = order.getOrderStatus();
        order.setPaymentStatus(PaymentStatus.PAID);
        order.setOrderStatus(OrderStatus.CONFIRMED);
        orderRepository.save(order);

        // Record history
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .previousStatus(prevStatus)
                .newStatus(OrderStatus.CONFIRMED)
                .changedByUserId(userId)
                .notes("Payment confirmed via " + paymentProvider + " (Ref: " + request.getProviderPaymentId() + ")")
                .build();
        historyRepository.save(history);

        notificationService.createNotification(order.getUser().getId(), "Payment Successful",
                "Payment of ₹" + order.getTotalAmount() + " for order #" + order.getOrderNumber() +
                        " was successful. Your order is confirmed!",
                "PAYMENT");

        auditLogService.log(userId, "PAYMENT_SUCCESS", "CustomerOrder", order.getId(),
                "Payment verified for order " + order.getOrderNumber() + " with ref " + request.getProviderPaymentId());

        return mapToResponse(saved);
    }

    private PaymentResponse mapToResponse(Payment p) {
        return PaymentResponse.builder()
                .id(p.getId())
                .orderId(p.getOrder().getId())
                .provider(p.getProvider())
                .providerOrderId(p.getProviderOrderId())
                .providerPaymentId(p.getProviderPaymentId())
                .amount(p.getAmount())
                .currency(p.getCurrency())
                .status(p.getStatus())
                .createdAt(p.getCreatedAt())
                .build();
    }
}
