package com.medicareai.dto.ai;

import com.medicareai.enums.AiSender;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiMessageResponse {
    private Long id;
    private AiSender sender;
    private String message;
    private LocalDateTime createdAt;
}
