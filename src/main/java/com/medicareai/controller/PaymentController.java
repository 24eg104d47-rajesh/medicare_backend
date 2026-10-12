package com.medicareai.controller;

import com.medicareai.dto.common.ApiResponse;
import com.medicareai.dto.payment.PaymentInitiateRequest;
import com.medicareai.dto.payment.PaymentInitiateResponse;
import com.medicareai.dto.payment.PaymentResponse;
import com.medicareai.dto.payment.PaymentVerifyRequest;
import com.medicareai.security.UserDetailsImpl;
import com.medicareai.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/create")
    public ResponseEntity<ApiResponse<PaymentInitiateResponse>> createPayment(
            @Valid @RequestBody PaymentInitiateRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        PaymentInitiateResponse response = paymentService.initiatePayment(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Payment initiated", response));
    }

    @PostMapping("/{id}/verify")
    public ResponseEntity<ApiResponse<PaymentResponse>> verifyPayment(
            @PathVariable Long id,
            @Valid @RequestBody PaymentVerifyRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        request.setOrderId(id);
        PaymentResponse response = paymentService.verifyPayment(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Payment verified successfully", response));
    }

    @PostMapping("/webhook/{provider}")
    public ResponseEntity<ApiResponse<String>> handleWebhook(
            @PathVariable String provider,
            @RequestBody(required = false) Map<String, Object> payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {
        // Idempotent webhook receiver
        return ResponseEntity.ok(ApiResponse.ok("Webhook acknowledged", "EVENT_PROCESSED"));
    }
}
