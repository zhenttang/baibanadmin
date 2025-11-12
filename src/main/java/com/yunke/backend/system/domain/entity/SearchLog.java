package com.yunke.backend.system.domain.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.*;

import java.time.LocalDateTime;

/**
 * 搜索日志实体类
 * 记录用户的搜索行为，用于分析热门搜索和搜索建议
 */
@Entity
@Table(name = "search_logs")
@Data
@EqualsAndHashCode(callSuper = false)
public class SearchLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 用户ID，可为空（匿名搜索）
     */
    @Column(name = "user_id")
    private String userId;

    /**
     * 搜索关键词
     */
    @Column(name = "search_keyword")
    private String searchKeyword;

    /**
     * 搜索类型
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "search_type")
    private SearchType searchType;

    /**
     * 搜索结果数量
     */
    @Column(name = "result_count")
    private Integer resultCount;

    /**
     * 创建时间
     */
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDateTime createdAt;

    /**
     * 搜索类型枚举
     */
    public enum SearchType {
        TITLE("标题搜索"),
        CONTENT("内容搜索"),
        AUTHOR("作者搜索"),
        TAG("标签搜索"),
        CATEGORY("分类搜索"),
        COMPREHENSIVE("综合搜索");

        private final String description;

        SearchType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }
}