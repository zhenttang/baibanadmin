package com.yunke.backend.document.domain.entity;

/**
 * 文档模式枚举
 */
public enum DocMode {
    PAGE,       // 页面模式
    EDGELESS,   // 无边界模式
    BOTH;       // 两种模式都支持
    
    /**
     * 从整数值获取对应的模式枚举
     * @param value 模式值
     * @return 对应的DocMode枚举
     */
    public static DocMode fromValue(Integer value) {
        if (value == null) {
            return PAGE;
        }
        
        switch (value) {
            case 0:
                return PAGE;
            case 1:
                return EDGELESS;
            case 2:
                return BOTH;
            default:
                return PAGE;
        }
    }
} 