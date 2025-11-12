package com.yunke.backend.system.enums;

import com.fasterxml.jackson.annotation.JsonValue;

/**
 * 运行时配置类型枚举
 * 对应 Prisma Schema 中的 RuntimeConfigType
 */
public enum RuntimeConfigType {
    /**
     * 字符串类型
     */
    STRING("String"),
    
    /**
     * 数字类型
     */
    NUMBER("Number"),
    
    /**
     * 布尔类型
     */
    BOOLEAN("Boolean"),
    
    /**
     * 对象类型
     */
    OBJECT("Object"),
    
    /**
     * 数组类型
     */
    ARRAY("Array");

    private final String value;

    RuntimeConfigType(String value) {
        this.value = value;
    }

    @JsonValue
    public String getValue() {
        return value;
    }

    public static RuntimeConfigType fromValue(String value) {
        for (RuntimeConfigType type : RuntimeConfigType.values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown RuntimeConfigType: " + value);
    }
} 