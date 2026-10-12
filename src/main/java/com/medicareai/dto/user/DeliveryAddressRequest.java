package com.medicareai.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAddressRequest {
    @NotBlank(message = "Recipient name is required")
    private String recipientName;

    private String phone;
    private String phoneNumber;

    @NotBlank(message = "Address line 1 is required")
    private String addressLine1;

    private String addressLine2;

    @NotBlank(message = "City is required")
    private String city;

    private String state;

    @NotBlank(message = "Postal code is required")
    private String postalCode;

    @Builder.Default
    private String country = "India";

    @Builder.Default
    private boolean defaultAddress = false;

    private Boolean isDefault;
    private String addressType;

    public String getPhone() {
        if (phone != null && !phone.isBlank()) return phone.trim();
        if (phoneNumber != null && !phoneNumber.isBlank()) return phoneNumber.trim();
        return "";
    }

    public String getState() {
        if (state != null && !state.isBlank()) return state.trim();
        return "General";
    }

    public boolean isDefaultAddress() {
        if (isDefault != null) return isDefault;
        return defaultAddress;
    }
}
