package com.yunke.backend.security.enums;

/**
 * 安全事件类型枚举
 */
public enum SecurityEventType {
    /**
     * DDoS攻击
     */
    DDOS,
    
    /**
     * 暴力破解
     */
    BRUTE_FORCE,
    
    /**
     * SQL注入尝试
     */
    SQL_INJECTION,
    
    /**
     * XSS攻击尝试
     */
    XSS_ATTACK,
    
    /**
     * 恶意爬虫
     */
    BOT,
    
    /**
     * API滥用
     */
    API_ABUSE,
    
    /**
     * 异常登录
     */
    SUSPICIOUS_LOGIN,
    
    /**
     * 越权访问
     */
    UNAUTHORIZED_ACCESS
}

