package com.yunke.backend.system.service;

import com.yunke.backend.common.dto.PaginationInput;
import com.yunke.backend.common.dto.PaginatedResponse;
import com.yunke.backend.document.dto.DocDto;
import com.yunke.backend.search.dto.AggregateInputDto;
import com.yunke.backend.search.dto.AggregateResultDto;
import com.yunke.backend.system.dto.SearchInputDto;
import com.yunke.backend.system.dto.SearchResultDto;
import com.yunke.backend.system.dto.IndexStatsDto;
import com.yunke.backend.system.dto.SearchSuggestionDto;
import com.yunke.backend.system.dto.SearchHistoryDto;
import com.yunke.backend.system.dto.SearchHealthDto;
import com.yunke.backend.search.enums.SearchTable;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 搜索服务接口
 * 扩展支持完整的搜索功能
 */
public interface SearchService {

    // ==================== 原有方法 ====================

    /**
     * 搜索文档
     */
    Mono<PaginatedResponse<DocDto>> searchDocs(String workspaceId, String query, PaginationInput pagination, String userId);

    /**
     * 全文搜索
     */
    Mono<SearchResult> search(String query, SearchOptions options);

    /**
     * 索引文档
     */
    Mono<Void> indexDocument(String docId, String title, String content, Map<String, Object> metadata);

    /**
     * 删除文档索引
     */
    Mono<Void> deleteDocument(String docId);

    /**
     * 简化删除文档方法
     */
    default Mono<Boolean> deleteDoc(String workspaceId, String docId) {
        return deleteDocument(docId).then(Mono.just(true));
    }

    /**
     * 更新文档索引
     */
    Mono<Void> updateDocument(String docId, String title, String content, Map<String, Object> metadata);

    /**
     * 重建索引
     */
    Mono<Void> rebuildIndex(String workspaceId);

    /**
     * 获取搜索建议
     */
    Mono<List<String>> getSuggestions(String query, String workspaceId);

    // ==================== 新增高级搜索方法 ====================

    /**
     * 高级搜索 - 支持复杂查询
     */
    Mono<SearchResultDto> advancedSearch(String workspaceId, SearchInputDto searchInput, String userId);

    /**
     * 聚合搜索
     */
    Mono<AggregateResultDto> aggregate(String workspaceId, AggregateInputDto aggregateInput, String userId);

    /**
     * 索引工作空间的所有文档
     */
    Mono<IndexStatsDto> indexWorkspace(String workspaceId, String userId);

    /**
     * 索引单个文档的详细信息
     */
    Mono<Void> indexDocumentAdvanced(String workspaceId, String docId, String userId);

    /**
     * 删除工作空间的所有索引
     */
    Mono<Void> deleteWorkspaceIndex(String workspaceId, String userId);

    /**
     * 获取索引统计信息
     */
    Mono<IndexStatsDto> getIndexStats(String workspaceId, String userId);

    /**
     * 搜索块内容
     */
    Mono<SearchResultDto> searchBlocks(String workspaceId, SearchInputDto searchInput, String userId);

    /**
     * 自动完成搜索
     */
    Mono<List<SearchSuggestionDto>> autoComplete(String workspaceId, String query, int limit, String userId);

    /**
     * 搜索历史记录
     */
    Mono<List<SearchHistoryDto>> getSearchHistory(String userId, int limit);

    /**
     * 添加搜索历史
     */
    Mono<Void> addSearchHistory(String userId, String query, String workspaceId);

    /**
     * 清除搜索历史
     */
    Mono<Void> clearSearchHistory(String userId);

    /**
     * 健康检查
     */
    Mono<SearchHealthDto> getSearchHealth();

    /**
     * 搜索结果
     */
    record SearchResult(
            List<SearchHit> hits,
            long totalHits,
            double maxScore,
            long took,
            Map<String, Object> aggregations
    ) {}

    /**
     * 搜索命中
     */
    record SearchHit(
            String id,
            String title,
            String content,
            String type,
            double score,
            Map<String, Object> metadata,
            Map<String, List<String>> highlights
    ) {}

    /**
     * 搜索选项
     */
    record SearchOptions(
            String workspaceId,
            String userId,
            List<String> types,
            int from,
            int size,
            String sortBy,
            String sortOrder,
            Map<String, Object> filters,
            boolean highlight
    ) {}
}