package com.medicareai.service;

import com.medicareai.dto.auth.AuthResponse;
import com.medicareai.dto.auth.LoginRequest;
import com.medicareai.dto.auth.RegisterRequest;
import com.medicareai.dto.user.UpdateProfileRequest;
import com.medicareai.dto.user.UserProfileResponse;
import com.medicareai.entity.Cart;
import com.medicareai.entity.CustomerProfile;
import com.medicareai.entity.User;
import com.medicareai.enums.Role;
import com.medicareai.exception.BadRequestException;
import com.medicareai.exception.ResourceNotFoundException;
import com.medicareai.repository.CartRepository;
import com.medicareai.repository.CustomerProfileRepository;
import com.medicareai.repository.UserRepository;
import com.medicareai.security.JwtUtils;
import com.medicareai.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final CustomerProfileRepository customerProfileRepository;
    private final CartRepository cartRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final AuditLogService auditLogService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email is already registered: " + request.getEmail());
        }

        String resolvedFullName = request.getResolvedFullName();
        String resolvedPhone = request.getResolvedPhone();

        // Public registration is strictly restricted to CUSTOMER role
        User user = User.builder()
                .fullName(resolvedFullName)
                .email(request.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(resolvedPhone)
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        User savedUser = userRepository.save(user);

        // Initialize CustomerProfile
        CustomerProfile profile = CustomerProfile.builder()
                .user(savedUser)
                .build();
        customerProfileRepository.save(profile);

        // Initialize empty shopping Cart
        Cart cart = Cart.builder()
                .user(savedUser)
                .build();
        cartRepository.save(cart);

        auditLogService.log(savedUser.getId(), "USER_REGISTERED", "User", savedUser.getId(),
                "New customer registered: " + savedUser.getEmail());

        UserDetailsImpl userDetails = UserDetailsImpl.build(savedUser);
        String token = jwtUtils.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(savedUser.getId())
                .fullName(savedUser.getFullName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole().name())
                .build();
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail().toLowerCase().trim(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        String token = jwtUtils.generateToken(userDetails);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(userDetails.getId())
                .fullName(userDetails.getFullName())
                .email(userDetails.getUsername())
                .role(userDetails.getRole())
                .build();
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        CustomerProfile profile = customerProfileRepository.findByUserId(userId).orElse(null);

        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole().name())
                .enabled(user.isEnabled())
                .dateOfBirth(profile != null ? profile.getDateOfBirth() : null)
                .gender(profile != null ? profile.getGender() : null)
                .bloodGroup(profile != null ? profile.getBloodGroup() : null)
                .allergies(profile != null ? profile.getAllergies() : null)
                .chronicConditions(profile != null ? profile.getChronicConditions() : null)
                .emergencyContactName(profile != null ? profile.getEmergencyContactName() : null)
                .emergencyContactPhone(profile != null ? profile.getEmergencyContactPhone() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    @Transactional
    public UserProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName().trim());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone().trim());
        }
        userRepository.save(user);

        CustomerProfile profile = customerProfileRepository.findByUserId(userId)
                .orElseGet(() -> CustomerProfile.builder().user(user).build());

        if (request.getDateOfBirth() != null) {
            profile.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            profile.setGender(request.getGender());
        }
        if (request.getBloodGroup() != null) {
            profile.setBloodGroup(request.getBloodGroup());
        }
        if (request.getAllergies() != null) {
            profile.setAllergies(request.getAllergies());
        }
        if (request.getChronicConditions() != null) {
            profile.setChronicConditions(request.getChronicConditions());
        }
        if (request.getEmergencyContactName() != null) {
            profile.setEmergencyContactName(request.getEmergencyContactName());
        }
        if (request.getEmergencyContactPhone() != null) {
            profile.setEmergencyContactPhone(request.getEmergencyContactPhone());
        }
        customerProfileRepository.save(profile);

        auditLogService.log(userId, "PROFILE_UPDATED", "User", userId, "User profile updated");

        return getProfile(userId);
    }
}
