package com.yunke.backend.common.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * 验证结果DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ValidationResultDto {
    
    /**
     * 验证是否通过
     */
    private Boolean valid;
    
    /**
     * 验证消息
     */
    private String message;
    
    /**
     * 错误字段列表
     */
    private java.util.List<FieldErrorDto> fieldErrors;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FieldErrorDto {
        /**
         * 字段名称
         */
        private String field;
        
        /**
         * 错误消息
         */
        private String message;
        
        /**
         * 错误代码
         */
        private String code;
    }
    
    public static ValidationResultDto success() {
        return new ValidationResultDto(true, "验证通过", null);
    }
    
    public static ValidationResultDto failure(String message) {
        return new ValidationResultDto(false, message, null);
    }
}