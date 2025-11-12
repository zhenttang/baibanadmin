package com.yunke.backend.search.enums;

/**
 * 搜索提供者类型枚举
 * 对应Node.js版本的SearchProviderType
 */
public enum SearchProviderType {
    ELASTICSEARCH("elasticsearch"),
    MANTICORE_SEARCH("manticoresearch"),
    REDIS("redis");

    private final String value;

    SearchProviderType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SearchProviderType fromValue(String value) {
        for (SearchProviderType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown search provider type: " + value);
    }
}