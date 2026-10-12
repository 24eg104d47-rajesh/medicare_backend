package com.medicareai.dto.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserProfileResponse {
    private Long id;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private boolean enabled;
    private LocalDate dateOfBirth;
    private String gender;
    private String bloodGroup;
    private String allergies;
    private String chronicConditions;
    private String emergencyContactName;
    private String emergencyContactPhone;
    private LocalDateTime createdAt;
}
