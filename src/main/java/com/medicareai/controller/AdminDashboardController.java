package com.medicareai.controller;

import com.medicareai.dto.admin.AdminDashboardSummary;
import com.medicareai.dto.common.ApiResponse;
import com.medicareai.dto.common.PageResponse;
import com.medicareai.entity.AuditLog;
import com.medicareai.enums.Role;
import com.medicareai.entity.User;
import com.medicareai.exception.ResourceNotFoundException;
import com.medicareai.repository.UserRepository;
import com.medicareai.service.AdminAnalyticsService;
import com.medicareai.service.AuditLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminAnalyticsService analyticsService;
    private final AuditLogService auditLogService;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACY_ADMIN')")
    public ResponseEntity<ApiResponse<AdminDashboardSummary>> getDashboardSummary() {
        AdminDashboardSummary summary = analyticsService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }

    @GetMapping("/analytics/top-products")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACY_ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getTopProducts(
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> top = analyticsService.getTopSellingProducts(limit);
        return ResponseEntity.ok(ApiResponse.ok(top));
    }

    @GetMapping("/analytics/recent-orders")
    @PreAuthorize("hasAnyRole('ADMIN', 'PHARMACY_ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getRecentOrders(
            @RequestParam(defaultValue = "10") int limit) {
        List<Map<String, Object>> recent = analyticsService.getRecentOrders(limit);
        return ResponseEntity.ok(ApiResponse.ok(recent));
    }

    @GetMapping("/audit-logs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<AuditLog>>> getAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<AuditLog> pageResult = auditLogService.getAuditLogs(page, size);
        return ResponseEntity.ok(ApiResponse.ok(PageResponse.from(pageResult)));
    }

    @GetMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<Map<String, Object>> userDtos = users.stream().map(u -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", u.getId());
            map.put("fullName", u.getFullName());
            map.put("email", u.getEmail());
            map.put("phone", u.getPhone());
            map.put("role", u.getRole().name());
            map.put("enabled", u.isEnabled());
            map.put("createdAt", u.getCreatedAt());
            return map;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(ApiResponse.ok(userDtos));
    }

    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        String roleStr = body.get("role");
        if (roleStr != null) {
            user.setRole(Role.valueOf(roleStr));
            userRepository.save(user);
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("id", user.getId());
        resp.put("role", user.getRole().name());
        return ResponseEntity.ok(ApiResponse.ok("User role updated successfully", resp));
    }

    @PutMapping("/users/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserStatus(
            @PathVariable Long id,
            @RequestBody Map<String, Object> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        Boolean enabled = null;
        if (body.get("enabled") != null) {
            enabled = Boolean.valueOf(body.get("enabled").toString());
        } else if (body.get("active") != null) {
            enabled = Boolean.valueOf(body.get("active").toString());
        }
        if (enabled != null) {
            user.setEnabled(enabled);
            userRepository.save(user);
        }
        Map<String, Object> resp = new HashMap<>();
        resp.put("id", user.getId());
        resp.put("enabled", user.isEnabled());
        return ResponseEntity.ok(ApiResponse.ok("User status updated successfully", resp));
    }
}
