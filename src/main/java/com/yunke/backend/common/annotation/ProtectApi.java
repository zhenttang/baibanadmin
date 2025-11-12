package com.yunke.backend.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 通用API保护注解
 * 
 * 用于限制任意API的调用频率
 * 
 * 使用示例：
 * <pre>
 * &#64;PostMapping("/api/some/expensive-operation")
 * &#64;ProtectApi(limit = 10, timeWindowMinutes = 1)
 * public Result expensiveOperation() {
 *     // 耗时操作
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtectApi {
    
    /**
     * 限制次数
     */
    int limit();
    
    /**
     * 时间窗口（分钟）
     */
    int timeWindowMinutes() default 1;
    
    /**
     * 错误消息
     */
    String message() default "操作过于频繁，请稍后再试";
    
    /**
     * 是否按用户限制（默认按用户，false则按IP限制）
     */
    boolean perUser() default true;
}

