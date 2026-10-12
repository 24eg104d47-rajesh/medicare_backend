package com.medicareai.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterRequest {
    private String fullName;
    private String firstName;
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 50, message = "Password must be at least 6 characters")
    private String password;

    private String phone;
    private String phoneNumber;

    public String getResolvedFullName() {
        if (fullName != null && !fullName.trim().isEmpty()) {
            return fullName.trim();
        }
        String combined = ((firstName != null ? firstName.trim() : "") + " " + (lastName != null ? lastName.trim() : "")).trim();
        return combined.isEmpty() ? "New Customer" : combined;
    }

    public String getResolvedPhone() {
        if (phone != null && !phone.trim().isEmpty()) {
            return phone.trim();
        }
        return phoneNumber != null ? phoneNumber.trim() : null;
    }
}
