package com.medicareai.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DeliveryAddressResponse {
    private Long id;
    private String recipientName;
    private String phone;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private boolean defaultAddress;
    private LocalDateTime createdAt;

    public String getPhoneNumber() {
        return phone;
    }

    public boolean getIsDefault() {
        return defaultAddress;
    }
}
