package com.medicareai.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiChatRequest {
    private Long conversationId;

    @NotBlank(message = "Message cannot be empty")
    private String message;
}
