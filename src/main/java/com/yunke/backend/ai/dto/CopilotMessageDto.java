package com.yunke.backend.ai.dto;

import com.yunke.backend.ai.domain.entity.CopilotMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Copilot消息DTO
 * 对应Node.js版本的CopilotMessageType
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopilotMessageDto {

    private String messageId;
    
    private String sessionId;
    
    private CopilotMessage.MessageRole role;
    
    private String content;
    
    private List<Map<String, Object>> attachments;
    
    private Map<String, Object> params;
    
    private Integer tokens;
    
    private String finishReason;
    
    private LocalDateTime createdAt;
}