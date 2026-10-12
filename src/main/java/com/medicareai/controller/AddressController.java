package com.medicareai.controller;

import com.medicareai.dto.common.ApiResponse;
import com.medicareai.dto.user.DeliveryAddressRequest;
import com.medicareai.dto.user.DeliveryAddressResponse;
import com.medicareai.security.UserDetailsImpl;
import com.medicareai.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<DeliveryAddressResponse>>> getMyAddresses(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<DeliveryAddressResponse> addresses = addressService.getUserAddresses(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(addresses));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<DeliveryAddressResponse>> createAddress(
            @Valid @RequestBody DeliveryAddressRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        DeliveryAddressResponse created = addressService.createAddress(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Address added successfully", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<DeliveryAddressResponse>> updateAddress(
            @PathVariable Long id,
            @Valid @RequestBody DeliveryAddressRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        DeliveryAddressResponse updated = addressService.updateAddress(userDetails.getId(), id, request);
        return ResponseEntity.ok(ApiResponse.ok("Address updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        addressService.deleteAddress(userDetails.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Address deleted successfully", null));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<DeliveryAddressResponse>> setDefaultAddress(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        DeliveryAddressResponse updated = addressService.setDefaultAddress(userDetails.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Default address updated", updated));
    }
}
