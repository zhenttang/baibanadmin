package com.yunke.backend.ai.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI聊天完成响应DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatCompletionResponse {
    private String id;
    private String object;
    private Long created;
    private String model;
    private List<Choice> choices;
    private Usage usage;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        private Integer index;
        private Message message;
        private Delta delta;
        @JsonProperty("finish_reason")
        private String finishReason;
        
        public static class Builder {
            private Integer index;
            private Message message;
            private Delta delta;
            private String finishReason;
            
            public Builder index(Integer index) {
                this.index = index;
                return this;
            }
            
            public Builder message(Message message) {
                this.message = message;
                return this;
            }
            
            public Builder delta(Delta delta) {
                this.delta = delta;
                return this;
            }
            
            public Builder finishReason(String finishReason) {
                this.finishReason = finishReason;
                return this;
            }
            
            public Choice build() {
                Choice choice = new Choice();
                choice.index = this.index;
                choice.message = this.message;
                choice.delta = this.delta;
                choice.finishReason = this.finishReason;
                return choice;
            }
        }
        
        public static Builder builder() {
            return new Builder();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
        
        public static class Builder {
            private String role;
            private String content;
            
            public Builder role(String role) {
                this.role = role;
                return this;
            }
            
            public Builder content(String content) {
                this.content = content;
                return this;
            }
            
            public Message build() {
                Message message = new Message();
                message.role = this.role;
                message.content = this.content;
                return message;
            }
        }
        
        public static Builder builder() {
            return new Builder();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Usage {
        @JsonProperty("prompt_tokens")
        private Integer promptTokens;
        @JsonProperty("completion_tokens")
        private Integer completionTokens;
        @JsonProperty("total_tokens")
        private Integer totalTokens;
        
        public static class Builder {
            private Integer promptTokens;
            private Integer completionTokens;
            private Integer totalTokens;
            
            public Builder promptTokens(Integer promptTokens) {
                this.promptTokens = promptTokens;
                return this;
            }
            
            public Builder completionTokens(Integer completionTokens) {
                this.completionTokens = completionTokens;
                return this;
            }
            
            public Builder totalTokens(Integer totalTokens) {
                this.totalTokens = totalTokens;
                return this;
            }
            
            public Usage build() {
                Usage usage = new Usage();
                usage.promptTokens = this.promptTokens;
                usage.completionTokens = this.completionTokens;
                usage.totalTokens = this.totalTokens;
                return usage;
            }
        }
        
        public static Builder builder() {
            return new Builder();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Delta {
        private String role;
        private String content;
        
        public static class Builder {
            private String role;
            private String content;
            
            public Builder role(String role) {
                this.role = role;
                return this;
            }
            
            public Builder content(String content) {
                this.content = content;
                return this;
            }
            
            public Delta build() {
                Delta delta = new Delta();
                delta.role = this.role;
                delta.content = this.content;
                return delta;
            }
        }
        
        public static Builder builder() {
            return new Builder();
        }
    }

    /**
     * Builder模式实现
     */
    public static class Builder {
        private String id;
        private String model;
        private List<ChatCompletionResponse.Choice> choices;
        private ChatCompletionResponse.Usage usage;
        private String object;
        private Long created;
        
        public Builder id(String id) {
            this.id = id;
            return this;
        }
        
        public Builder model(String model) {
            this.model = model;
            return this;
        }
        
        public Builder choices(List<ChatCompletionResponse.Choice> choices) {
            this.choices = choices;
            return this;
        }
        
        public Builder usage(ChatCompletionResponse.Usage usage) {
            this.usage = usage;
            return this;
        }
        
        public Builder object(String object) {
            this.object = object;
            return this;
        }
        
        public Builder created(Long created) {
            this.created = created;
            return this;
        }
        
        public ChatCompletionResponse build() {
            ChatCompletionResponse response = new ChatCompletionResponse();
            response.id = this.id;
            response.model = this.model;
            response.choices = this.choices;
            response.usage = this.usage;
            response.object = this.object;
            response.created = this.created;
            return response;
        }
    }
    
    public static Builder builder() {
        return new Builder();
    }
}