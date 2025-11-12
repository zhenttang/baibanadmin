package com.yunke.backend.document.dto;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 公开文档模式枚举
 */
public enum PublicDocMode {
    PAGE("page"),
    EDGELESS("edgeless"), 
    APPEND_ONLY("appendOnly");
    
    private final String value;
    
    PublicDocMode(String value) {
        this.value = value;
    }
    
    @JsonValue
    public String getValue() {
        return value;
    }
    
    public static PublicDocMode fromValue(String value) {
        for (PublicDocMode mode : values()) {
            if (mode.value.equals(value)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Invalid PublicDocMode value: " + value);
    }
}