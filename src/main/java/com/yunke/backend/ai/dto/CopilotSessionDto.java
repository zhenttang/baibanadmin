package com.yunke.backend.ai.dto;

import com.yunke.backend.ai.domain.entity.CopilotSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Copilot会话DTO
 * 对应Node.js版本的CopilotSessionType
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CopilotSessionDto {

    private String sessionId;
    
    private String userId;
    
    private String workspaceId;
    
    private String docId;
    
    private String title;
    
    private CopilotSession.AIProvider provider;
    
    private String model;
    
    private String prompt;
    
    private CopilotSession.SessionStatus status;
    
    private Integer tokensUsed;
    
    private Integer messageCount;
    
    private Map<String, Object> config;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime finishedAt;
}