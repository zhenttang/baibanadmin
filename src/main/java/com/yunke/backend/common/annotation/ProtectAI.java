package com.yunke.backend.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * AI接口保护注解
 * 
 * 用于限制AI接口的调用频率，防止滥用
 * 
 * 使用示例：
 * <pre>
 * &#64;PostMapping("/api/copilot/chat")
 * &#64;ProtectAI(limit = 20, timeUnit = TimeUnit.HOURS)
 * public Result chat(@RequestBody ChatRequest request) {
 *     // AI处理逻辑
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtectAI {
    
    /**
     * 限制次数
     */
    int limit() default 20;
    
    /**
     * 时间窗口（分钟）
     */
    int timeWindowMinutes() default 60;
    
    /**
     * 错误消息
     */
    String message() default "AI调用次数已达上限，请稍后再试";
}

