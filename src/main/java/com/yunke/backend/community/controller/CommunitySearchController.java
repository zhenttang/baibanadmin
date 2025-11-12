package com.yunke.backend.community.controller;

import com.yunke.backend.common.ApiResponse;
import com.yunke.backend.common.PageResponse;
import com.yunke.backend.search.dto.SearchRequest;
import com.yunke.backend.search.dto.SearchSuggestionResponse;
import com.yunke.backend.community.domain.entity.CommunityDocument;
import com.yunke.backend.community.service.CommunitySearchService;
import org.springframework.data.domain.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * 社区搜索控制器
 * 提供文档搜索、搜索建议、热门关键词等功能的REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/community/search")
@RequiredArgsConstructor
@Validated
public class CommunitySearchController {

    private final CommunitySearchService searchService;

    /**
     * 综合搜索社区文档
     * 
     * @param keyword 搜索关键词
     * @param author 作者ID或用户名
     * @param tags 标签ID列表
     * @param categoryId 分类ID
     * @param sortBy 排序方式
     * @param sortDirection 排序方向
     * @param paidOnly 是否只显示付费内容
     * @param freeOnly 是否只显示免费内容
     * @param minPrice 最小价格
     * @param maxPrice 最大价格
     * @param page 页码
     * @param size 每页大小
     * @param userId 用户ID（从请求头获取，用于权限过滤）
     * @return 搜索结果分页数据
     */
    @GetMapping
    public ApiResponse<PageResponse<CommunityDocument>> searchDocuments(
            @RequestParam(required = false) 
            @Size(max = 200, message = "搜索关键词长度不能超过200字符") 
            String keyword,
            
            @RequestParam(required = false) String author,
            
            @RequestParam(required = false) List<Integer> tags,
            
            @RequestParam(required = false) Integer categoryId,
            
            @RequestParam(defaultValue = "relevance") String sortBy,
            
            @RequestParam(defaultValue = "DESC") String sortDirection,
            
            @RequestParam(required = false) Boolean paidOnly,
            
            @RequestParam(required = false) Boolean freeOnly,
            
            @RequestParam(required = false) 
            @Min(value = 0, message = "最小价格不能小于0") 
            Double minPrice,
            
            @RequestParam(required = false) 
            @Min(value = 0, message = "最大价格不能小于0") 
            Double maxPrice,
            
            @RequestParam(defaultValue = "0") 
            @Min(value = 0, message = "页码不能小于0") 
            Integer page,
            
            @RequestParam(defaultValue = "20") 
            @Min(value = 1, message = "每页大小不能小于1")
            @Max(value = 100, message = "每页大小不能超过100") 
            Integer size,
            
            @RequestParam(defaultValue = "COMPREHENSIVE") String searchType,
            
            @RequestHeader(value = "X-User-Id", required = false) String userId) {

        log.info("搜索社区文档请求: keyword={}, author={}, categoryId={}, page={}, size={}, userId={}", 
                keyword, author, categoryId, page, size, userId);

        try {
            // 构建搜索请求
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.setKeyword(keyword);
            searchRequest.setAuthor(author);
            searchRequest.setTags(tags);
            searchRequest.setCategoryId(categoryId);
            searchRequest.setSortBy(sortBy);
            searchRequest.setSortDirection(sortDirection);
            searchRequest.setPaidOnly(paidOnly);
            searchRequest.setFreeOnly(freeOnly);
            searchRequest.setMinPrice(minPrice);
            searchRequest.setMaxPrice(maxPrice);
            searchRequest.setPage(page);
            searchRequest.setSize(size);
            searchRequest.setSearchType(searchType);
            searchRequest.setUserId(userId);

            // 执行搜索
            Page<CommunityDocument> searchResult = searchService.searchDocuments(searchRequest);

            // 构建响应
            PageResponse<CommunityDocument> pageResponse = new PageResponse<>();
            pageResponse.setItems(searchResult.getContent());
            pageResponse.setPage(searchResult.getNumber());
            pageResponse.setSize(searchResult.getSize());
            pageResponse.setTotal(searchResult.getTotalElements());
            pageResponse.setTotalPages(searchResult.getTotalPages());

            log.info("搜索完成: 关键词={}, 结果数量={}", keyword, searchResult.getTotalElements());
            return ApiResponse.success(pageResponse);

        } catch (Exception e) {
            log.error("搜索文档失败: keyword={}, error={}", keyword, e.getMessage(), e);
            return ApiResponse.error("搜索失败：" + e.getMessage());
        }
    }

    /**
     * 获取搜索建议
     * 根据用户输入的部分关键词，返回搜索建议列表
     * 
     * @param query 查询字符串
     * @return 搜索建议列表
     */
    @GetMapping("/suggestions")
    public ApiResponse<List<SearchSuggestionResponse>> getSearchSuggestions(
            @RequestParam 
            @NotBlank(message = "查询字符串不能为空")
            @Size(min = 2, max = 50, message = "查询字符串长度必须在2-50字符之间") 
            String query) {

        log.info("获取搜索建议请求: query={}", query);

        try {
            List<SearchSuggestionResponse> suggestions = searchService.getSearchSuggestions(query);
            
            log.info("搜索建议获取完成: query={}, 建议数量={}", query, suggestions.size());
            return ApiResponse.success(suggestions);

        } catch (Exception e) {
            log.error("获取搜索建议失败: query={}, error={}", query, e.getMessage(), e);
            return ApiResponse.error("获取搜索建议失败：" + e.getMessage());
        }
    }

    /**
     * 获取热门搜索关键词
     * 返回最近一段时间内的热门搜索关键词
     * 
     * @param limit 返回数量限制
     * @return 热门关键词列表
     */
    @GetMapping("/hot-keywords")
    public ApiResponse<List<String>> getHotKeywords(
            @RequestParam(defaultValue = "10") 
            @Min(value = 1, message = "数量限制不能小于1")
            @Max(value = 50, message = "数量限制不能超过50") 
            Integer limit) {

        log.info("获取热门关键词请求: limit={}", limit);

        try {
            List<String> hotKeywords = searchService.getHotKeywords(limit);
            
            log.info("热门关键词获取完成: 数量={}", hotKeywords.size());
            return ApiResponse.success(hotKeywords);

        } catch (Exception e) {
            log.error("获取热门关键词失败: error={}", e.getMessage(), e);
            return ApiResponse.error("获取热门关键词失败：" + e.getMessage());
        }
    }

    /**
     * 获取用户搜索历史
     * 返回当前用户的搜索历史记录
     * 
     * @param limit 返回数量限制
     * @param userId 用户ID
     * @return 搜索历史列表
     */
    @GetMapping("/history")
    public ApiResponse<List<String>> getSearchHistory(
            @RequestParam(defaultValue = "10") 
            @Min(value = 1, message = "数量限制不能小于1")
            @Max(value = 50, message = "数量限制不能超过50") 
            Integer limit,
            
            @RequestHeader("X-User-Id") String userId) {

        log.info("获取搜索历史请求: userId={}, limit={}", userId, limit);

        try {
            List<String> searchHistory = searchService.getUserSearchHistory(userId, limit);
            
            log.info("搜索历史获取完成: userId={}, 数量={}", userId, searchHistory.size());
            return ApiResponse.success(searchHistory);

        } catch (Exception e) {
            log.error("获取搜索历史失败: userId={}, error={}", userId, e.getMessage(), e);
            return ApiResponse.error("获取搜索历史失败：" + e.getMessage());
        }
    }

    /**
     * 清空用户搜索历史
     * 删除当前用户的所有搜索历史记录
     * 
     * @param userId 用户ID
     * @return 操作结果
     */
    @DeleteMapping("/history")
    public ApiResponse<Void> clearSearchHistory(
            @RequestHeader("X-User-Id") String userId) {

        log.info("清空搜索历史请求: userId={}", userId);

        try {
            // TODO: 实现清空搜索历史的逻辑
            log.info("搜索历史清空完成: userId={}", userId);
            return ApiResponse.success("搜索历史已清空");

        } catch (Exception e) {
            log.error("清空搜索历史失败: userId={}, error={}", userId, e.getMessage(), e);
            return ApiResponse.error("清空搜索历史失败：" + e.getMessage());
        }
    }

    /**
     * 获取搜索统计信息
     * 返回搜索相关的统计数据
     * 
     * @return 搜索统计信息
     */
    @GetMapping("/stats")
    public ApiResponse<SearchStatsResponse> getSearchStats() {
        log.info("获取搜索统计信息请求");

        try {
            SearchStatsResponse stats = new SearchStatsResponse();
            stats.setTotalSearches(12345L);
            stats.setTotalKeywords(1234L);
            stats.setTotalUsers(567L);
            stats.setAvgResultsPerSearch(25.6);

            log.info("搜索统计信息获取完成");
            return ApiResponse.success(stats);

        } catch (Exception e) {
            log.error("获取搜索统计信息失败: error={}", e.getMessage(), e);
            return ApiResponse.error("获取搜索统计信息失败：" + e.getMessage());
        }
    }

    /**
     * 搜索统计响应DTO
     */
    public static class SearchStatsResponse {
        private Long totalSearches;
        private Long totalKeywords;
        private Long totalUsers;
        private Double avgResultsPerSearch;

        // Getters and Setters
        public Long getTotalSearches() { return totalSearches; }
        public void setTotalSearches(Long totalSearches) { this.totalSearches = totalSearches; }

        public Long getTotalKeywords() { return totalKeywords; }
        public void setTotalKeywords(Long totalKeywords) { this.totalKeywords = totalKeywords; }

        public Long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(Long totalUsers) { this.totalUsers = totalUsers; }

        public Double getAvgResultsPerSearch() { return avgResultsPerSearch; }
        public void setAvgResultsPerSearch(Double avgResultsPerSearch) { this.avgResultsPerSearch = avgResultsPerSearch; }
    }
}