package com.yunke.backend.ai.dto;

import com.yunke.backend.ai.domain.entity.CopilotSession;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 创建聊天会话输入DTO
 * 对应Node.js版本的CreateChatSessionInput
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatSessionInput {

    private String workspaceId;
    
    private String docId;
    
    private String title;
    
    private CopilotSession.AIProvider provider;
    
    private String model;
    
    private String prompt;
    
    private Map<String, Object> config;
}