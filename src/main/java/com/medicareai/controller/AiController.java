package com.medicareai.controller;

import com.medicareai.dto.ai.*;
import com.medicareai.dto.common.ApiResponse;
import com.medicareai.security.UserDetailsImpl;
import com.medicareai.service.AiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping(value = {"/chat", "/ask"})
    public ResponseEntity<ApiResponse<AiChatResponse>> chat(
            @Valid @RequestBody AiChatRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        AiChatResponse response = aiService.chat(userDetails.getId(), request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/conversations")
    public ResponseEntity<ApiResponse<List<AiConversationResponse>>> getConversations(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        List<AiConversationResponse> history = aiService.getConversationHistory(userDetails.getId());
        return ResponseEntity.ok(ApiResponse.ok(history));
    }

    @GetMapping("/conversations/{id}")
    public ResponseEntity<ApiResponse<AiConversationResponse>> getConversationById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        AiConversationResponse conversation = aiService.getConversationById(userDetails.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok(conversation));
    }

    @DeleteMapping("/conversations/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteConversation(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        aiService.deleteConversation(userDetails.getId(), id);
        return ResponseEntity.ok(ApiResponse.ok("Conversation deleted", null));
    }

    @PostMapping("/medicine-information")
    public ResponseEntity<ApiResponse<AiMedicineInfoResponse>> getMedicineInformation(
            @Valid @RequestBody AiMedicineInfoRequest request) {
        AiMedicineInfoResponse response = aiService.getMedicineInformation(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/product-discovery")
    public ResponseEntity<ApiResponse<AiProductDiscoveryResponse>> discoverProducts(
            @Valid @RequestBody AiProductDiscoveryRequest request) {
        AiProductDiscoveryResponse response = aiService.discoverProducts(request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
