package com.yunke.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * 聊天完成请求DTO
 * 对应OpenAI Chat Completion API格式
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionRequest {

    private String model;

    private List<ChatMessage> messages;

    @JsonProperty("max_tokens")
    private Integer maxTokens;

    private Double temperature;

    @JsonProperty("top_p")
    private Double topP;

    private Integer n;

    private Boolean stream;

    private List<String> stop;

    @JsonProperty("presence_penalty")
    private Double presencePenalty;

    @JsonProperty("frequency_penalty")
    private Double frequencyPenalty;

    @JsonProperty("logit_bias")
    private Map<String, Object> logitBias;

    private String user;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatMessage {
        private String role;
        private String content;
        private String name;
        private Map<String, Object> functionCall;
    }
}