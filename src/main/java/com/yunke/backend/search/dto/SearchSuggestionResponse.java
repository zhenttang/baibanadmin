package com.yunke.backend.search.dto;

import lombok.Data;

/**
 * 搜索建议响应DTO
 */
@Data
public class SearchSuggestionResponse {

    /**
     * 建议的搜索词
     */
    private String suggestion;

    /**
     * 该搜索词的搜索次数
     */
    private Long searchCount;

    /**
     * 搜索类型
     */
    private String type;

    /**
     * 相关度评分
     */
    private Double score;

    public SearchSuggestionResponse(String suggestion, Long searchCount, String type) {
        this.suggestion = suggestion;
        this.searchCount = searchCount;
        this.type = type;
        this.score = calculateScore(searchCount);
    }

    /**
     * 根据搜索次数计算相关度评分
     */
    private Double calculateScore(Long count) {
        if (count == null || count == 0) {
            return 0.0;
        }
        // 使用对数函数计算评分，避免热门词汇评分过高
        return Math.log10(count.doubleValue() + 1) * 10;
    }
}

/**
 * 热门关键词响应DTO
 */
@Data
class HotKeywordResponse {

    /**
     * 关键词
     */
    private String keyword;

    /**
     * 搜索次数
     */
    private Long searchCount;

    /**
     * 结果数量平均值
     */
    private Double avgResultCount;

    /**
     * 最近搜索时间
     */
    private String lastSearchTime;

    /**
     * 趋势（上升/下降/持平）
     */
    private String trend;

    public HotKeywordResponse(String keyword, Long searchCount, Double avgResultCount) {
        this.keyword = keyword;
        this.searchCount = searchCount;
        this.avgResultCount = avgResultCount;
    }
}