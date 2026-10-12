package com.medicareai.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiConversationResponse {
    private Long id;
    private Long userId;
    private String title;
    @Builder.Default
    private List<AiMessageResponse> messages = new ArrayList<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
