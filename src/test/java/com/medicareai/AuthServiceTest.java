package com.medicareai;

import com.medicareai.dto.auth.AuthResponse;
import com.medicareai.dto.auth.LoginRequest;
import com.medicareai.dto.auth.RegisterRequest;
import com.medicareai.entity.CustomerProfile;
import com.medicareai.entity.User;
import com.medicareai.enums.Role;
import com.medicareai.exception.BadRequestException;
import com.medicareai.repository.CartRepository;
import com.medicareai.repository.CustomerProfileRepository;
import com.medicareai.repository.UserRepository;
import com.medicareai.security.JwtUtils;
import com.medicareai.security.UserDetailsImpl;
import com.medicareai.service.AuditLogService;
import com.medicareai.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private CustomerProfileRepository customerProfileRepository;

    @Mock
    private CartRepository cartRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtUtils jwtUtils;

    @Mock
    private AuditLogService auditLogService;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = RegisterRequest.builder()
                .fullName("Test Customer")
                .email("test@medicare.ai")
                .password("Password@123")
                .phone("+91 9999999999")
                .build();
    }

    @Test
    void testRegisterSuccess() {
        when(userRepository.existsByEmail("test@medicare.ai")).thenReturn(false);
        when(passwordEncoder.encode("Password@123")).thenReturn("encoded_pass");

        User savedUser = User.builder()
                .id(1L)
                .fullName("Test Customer")
                .email("test@medicare.ai")
                .password("encoded_pass")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtUtils.generateToken(any(UserDetailsImpl.class))).thenReturn("mock_jwt_token");

        AuthResponse response = authService.register(registerRequest);

        assertNotNull(response);
        assertEquals("test@medicare.ai", response.getEmail());
        assertEquals("CUSTOMER", response.getRole());
        assertEquals("mock_jwt_token", response.getToken());
        verify(userRepository, times(1)).save(any(User.class));
        verify(customerProfileRepository, times(1)).save(any(CustomerProfile.class));
    }

    @Test
    void testRegisterDuplicateEmailThrowsException() {
        when(userRepository.existsByEmail("test@medicare.ai")).thenReturn(true);

        assertThrows(BadRequestException.class, () -> authService.register(registerRequest));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLoginSuccess() {
        LoginRequest loginRequest = LoginRequest.builder()
                .email("test@medicare.ai")
                .password("Password@123")
                .build();

        User user = User.builder()
                .id(1L)
                .fullName("Test Customer")
                .email("test@medicare.ai")
                .password("encoded_pass")
                .role(Role.CUSTOMER)
                .enabled(true)
                .build();
        UserDetailsImpl userDetails = UserDetailsImpl.build(user);

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(auth);
        when(jwtUtils.generateToken(userDetails)).thenReturn("login_jwt_token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("login_jwt_token", response.getToken());
        assertEquals("CUSTOMER", response.getRole());
    }
}
