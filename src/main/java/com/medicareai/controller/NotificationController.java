package com.medicareai.controller;

import com.medicareai.dto.common.ApiResponse;
import com.medicareai.dto.notification.NotificationResponse;
import com.medicareai.security.UserDetailsImpl;
import com.medicareai.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<NotificationResponse> list = notificationService.getUserNotifications(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        notificationService.markAsRead(userDetails.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Marked as read", null));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        notificationService.markAllAsRead(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok("All marked as read", null));
    }
}
