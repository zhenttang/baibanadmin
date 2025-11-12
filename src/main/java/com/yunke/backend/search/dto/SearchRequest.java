package com.yunke.backend.search.dto;

import lombok.Data;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 搜索请求DTO
 */
@Data
public class SearchRequest {

    /**
     * 搜索关键词
     */
    @Size(max = 200, message = "搜索关键词长度不能超过200字符")
    private String keyword;

    /**
     * 作者ID或用户名
     */
    private String author;

    /**
     * 标签ID列表
     */
    private List<Integer> tags;

    /**
     * 分类ID
     */
    private Integer categoryId;

    /**
     * 排序方式
     * created_at: 按创建时间排序
     * view_count: 按浏览量排序
     * like_count: 按点赞数排序
     * collect_count: 按收藏数排序
     * relevance: 按相关度排序（默认）
     */
    private String sortBy = "relevance";

    /**
     * 排序方向 ASC/DESC
     */
    private String sortDirection = "DESC";

    /**
     * 是否只显示付费内容
     */
    private Boolean paidOnly;

    /**
     * 是否只显示免费内容
     */
    private Boolean freeOnly;

    /**
     * 价格范围 - 最小值
     */
    @Min(value = 0, message = "最小价格不能小于0")
    private Double minPrice;

    /**
     * 价格范围 - 最大值
     */
    @Min(value = 0, message = "最大价格不能小于0")
    private Double maxPrice;

    /**
     * 页码
     */
    @Min(value = 0, message = "页码不能小于0")
    private Integer page = 0;

    /**
     * 每页大小
     */
    @Min(value = 1, message = "每页大小不能小于1")
    @Max(value = 100, message = "每页大小不能超过100")
    private Integer size = 20;

    /**
     * 用户ID（用于权限过滤）
     */
    private String userId;

    /**
     * 搜索类型
     */
    private String searchType = "COMPREHENSIVE";
}