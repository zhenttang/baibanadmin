package com.yunke.backend.ai.dto;

import com.yunke.backend.ai.domain.entity.CopilotMessage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 创建聊天消息输入DTO
 * 对应Node.js版本的CreateChatMessageInput
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateChatMessageInput {

    private String sessionId;
    
    private String content;
    
    private CopilotMessage.MessageRole role;
    
    private List<MessageAttachment> attachments;
    
    private Map<String, Object> params;
    
    private Boolean stream;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageAttachment {
        private String type;
        private String name;
        private String content;
        private Long size;
        private String mimeType;
    }
}