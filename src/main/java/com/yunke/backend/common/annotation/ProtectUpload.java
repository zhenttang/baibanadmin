package com.yunke.backend.common.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 文件上传保护注解
 * 
 * 用于限制文件上传的频率，防止滥用
 * 
 * 使用示例：
 * <pre>
 * &#64;PostMapping("/api/blobs/upload")
 * &#64;ProtectUpload(limit = 50, timeWindowMinutes = 60)
 * public Result upload(@RequestParam("file") MultipartFile file) {
 *     // 上传逻辑
 * }
 * </pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ProtectUpload {
    
    /**
     * 限制次数
     */
    int limit() default 50;
    
    /**
     * 时间窗口（分钟）
     */
    int timeWindowMinutes() default 60;
    
    /**
     * 错误消息
     */
    String message() default "上传次数已达上限，请稍后再试";
}

