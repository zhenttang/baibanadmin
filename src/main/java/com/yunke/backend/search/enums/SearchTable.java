package com.yunke.backend.search.enums;

/**
 * 搜索表类型枚举
 * 对应Node.js版本的SearchTable
 */
public enum SearchTable {
    DOC("doc"),
    BLOCK("block");

    private final String value;

    SearchTable(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static SearchTable fromValue(String value) {
        for (SearchTable table : values()) {
            if (table.value.equals(value)) {
                return table;
            }
        }
        throw new IllegalArgumentException("Unknown search table: " + value);
    }
}