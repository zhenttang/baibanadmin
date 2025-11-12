package com.yunke.backend.search.enums;

/**
 * 搜索查询布尔操作符枚举
 * 对应Node.js版本的SearchQueryOccur
 */
public enum SearchQueryOccur {
    SHOULD("should"),    // OR操作
    MUST("must"),        // AND操作
    MUST_NOT("must_not"); // NOT操作

    private final String value;

    SearchQueryOccur(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SearchQueryOccur fromValue(String value) {
        for (SearchQueryOccur occur : values()) {
            if (occur.value.equals(value)) {
                return occur;
            }
        }
        throw new IllegalArgumentException("Unknown search query occur: " + value);
    }
}