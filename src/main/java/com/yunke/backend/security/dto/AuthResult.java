package com.yunke.backend.security.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 认证结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResult {
    private String userId;
    private String email;
    private String name;
    private String token;
    private String refreshToken;
    private long expiresIn;
    private boolean isNewUser;
    
    // 手动添加builder方法 - 临时解决Lombok编译问题
    public static AuthResultBuilder builder() {
        return new AuthResultBuilder();
    }
    
    public static class AuthResultBuilder {
        private String userId;
        private String email;
        private String name;
        private String token;
        private String refreshToken;
        private long expiresIn;
        private boolean isNewUser;
        
        public AuthResultBuilder userId(String userId) { this.userId = userId; return this; }
        public AuthResultBuilder email(String email) { this.email = email; return this; }
        public AuthResultBuilder name(String name) { this.name = name; return this; }
        public AuthResultBuilder token(String token) { this.token = token; return this; }
        public AuthResultBuilder refreshToken(String refreshToken) { this.refreshToken = refreshToken; return this; }
        public AuthResultBuilder expiresIn(long expiresIn) { this.expiresIn = expiresIn; return this; }
        public AuthResultBuilder isNewUser(boolean isNewUser) { this.isNewUser = isNewUser; return this; }
        
        public AuthResult build() {
            AuthResult result = new AuthResult();
            result.userId = this.userId;
            result.email = this.email;
            result.name = this.name;
            result.token = this.token;
            result.refreshToken = this.refreshToken;
            result.expiresIn = this.expiresIn;
            result.isNewUser = this.isNewUser;
            return result;
        }
    }
} 