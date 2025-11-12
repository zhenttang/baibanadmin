package com.yunke.backend.security.dto.security;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录检查结果DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginCheckResult {
    
    /**
     * 是否允许登录
     */
    private boolean allowed;
    
    /**
     * 是否被封禁
     */
    private boolean blocked;
    
    /**
     * 是否需要验证码
     */
    private boolean needCaptcha;
    
    /**
     * 消息
     */
    private String message;
    
    /**
     * 剩余失败次数
     */
    private Integer remainingAttempts;
    
    public static LoginCheckResult allowed() {
        return LoginCheckResult.builder()
                .allowed(true)
                .blocked(false)
                .needCaptcha(false)
                .build();
    }
    
    public static LoginCheckResult blocked(String message) {
        return LoginCheckResult.builder()
                .allowed(false)
                .blocked(true)
                .needCaptcha(false)
                .message(message)
                .build();
    }
    
    public static LoginCheckResult needCaptcha(String message) {
        return LoginCheckResult.builder()
                .allowed(false)
                .blocked(false)
                .needCaptcha(true)
                .message(message)
                .build();
    }
}

