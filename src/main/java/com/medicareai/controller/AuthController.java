package com.medicareai.controller;

import com.medicareai.dto.auth.AuthResponse;
import com.medicareai.dto.auth.LoginRequest;
import com.medicareai.dto.auth.RegisterRequest;
import com.medicareai.dto.common.ApiResponse;
import com.medicareai.dto.user.UpdateProfileRequest;
import com.medicareai.dto.user.UserProfileResponse;
import com.medicareai.security.UserDetailsImpl;
import com.medicareai.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Registration successful", response));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", response));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getCurrentUser(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        UserProfileResponse profile = authService.getProfile(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(profile));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateProfile(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody UpdateProfileRequest request) {
        UserProfileResponse updated = authService.updateProfile(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok("Profile updated successfully", updated));
    }
}
