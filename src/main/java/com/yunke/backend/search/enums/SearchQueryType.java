package com.yunke.backend.search.enums;

/**
 * 搜索查询类型枚举
 * 对应Node.js版本的SearchQueryType
 */
public enum SearchQueryType {
    MATCH("match"),
    BOOST("boost"),
    BOOLEAN("boolean"),
    EXISTS("exists"),
    ALL("all");

    private final String value;

    SearchQueryType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SearchQueryType fromValue(String value) {
        for (SearchQueryType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown search query type: " + value);
    }
}