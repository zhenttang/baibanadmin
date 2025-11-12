package com.yunke.backend.system.controller;

import com.yunke.backend.search.dto.AggregateInputDto;
import com.yunke.backend.search.dto.AggregateResultDto;
import com.yunke.backend.system.dto.*;
import com.yunke.backend.system.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.security.Principal;
import java.util.List;

/**
 * 高级搜索控制器
 * 提供完整的搜索功能REST API
 */
@Slf4j
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/search")
@RequiredArgsConstructor
public class AdvancedSearchController {

    private final SearchService searchService;

    /**
     * 高级搜索
     * POST /api/workspaces/:workspaceId/search
     */
    @PostMapping
    public Mono<ResponseEntity<SearchResultDto>> advancedSearch(
            @PathVariable String workspaceId,
            @RequestBody SearchInputDto searchInput,
            Principal principal) {
        
        return searchService.advancedSearch(workspaceId, searchInput, principal.getName())
                .map(result -> ResponseEntity.ok(result))
                .onErrorResume(e -> {
                    log.error("Advanced search failed: workspace={}", workspaceId, e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 聚合搜索
     * POST /api/workspaces/:workspaceId/search/aggregate
     */
    @PostMapping("/aggregate")
    public Mono<ResponseEntity<AggregateResultDto>> aggregate(
            @PathVariable String workspaceId,
            @RequestBody AggregateInputDto aggregateInput,
            Principal principal) {
        
        return searchService.aggregate(workspaceId, aggregateInput, principal.getName())
                .map(result -> ResponseEntity.ok(result))
                .onErrorResume(e -> {
                    log.error("Aggregate search failed: workspace={}", workspaceId, e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 搜索块内容
     * POST /api/workspaces/:workspaceId/search/blocks
     */
    @PostMapping("/blocks")
    public Mono<ResponseEntity<SearchResultDto>> searchBlocks(
            @PathVariable String workspaceId,
            @RequestBody SearchInputDto searchInput,
            Principal principal) {
        
        return searchService.searchBlocks(workspaceId, searchInput, principal.getName())
                .map(result -> ResponseEntity.ok(result))
                .onErrorResume(e -> {
                    log.error("Block search failed: workspace={}", workspaceId, e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 自动完成
     * GET /api/workspaces/:workspaceId/search/autocomplete
     */
    @GetMapping("/autocomplete")
    public Mono<ResponseEntity<List<SearchSuggestionDto>>> autoComplete(
            @PathVariable String workspaceId,
            @RequestParam("q") String query,
            @RequestParam(value = "limit", defaultValue = "10") int limit,
            Principal principal) {
        
        return searchService.autoComplete(workspaceId, query, limit, principal.getName())
                .map(suggestions -> ResponseEntity.ok(suggestions))
                .onErrorResume(e -> {
                    log.error("Autocomplete failed: workspace={}, query={}", workspaceId, query, e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 重建工作空间索引
     * POST /api/workspaces/:workspaceId/search/reindex
     */
    @PostMapping("/reindex")
    public Mono<ResponseEntity<IndexStatsDto>> reindexWorkspace(
            @PathVariable String workspaceId,
            Principal principal) {
        
        return searchService.indexWorkspace(workspaceId, principal.getName())
                .map(stats -> ResponseEntity.ok(stats))
                .onErrorResume(e -> {
                    log.error("Reindex failed: workspace={}", workspaceId, e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 获取索引统计
     * GET /api/workspaces/:workspaceId/search/stats
     */
    @GetMapping("/stats")
    public Mono<ResponseEntity<IndexStatsDto>> getIndexStats(
            @PathVariable String workspaceId,
            Principal principal) {
        
        return searchService.getIndexStats(workspaceId, principal.getName())
                .map(stats -> ResponseEntity.ok(stats))
                .onErrorResume(e -> {
                    log.error("Get index stats failed: workspace={}", workspaceId, e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 删除工作空间索引
     * DELETE /api/workspaces/:workspaceId/search/index
     */
    @DeleteMapping("/index")
    public Mono<ResponseEntity<Boolean>> deleteWorkspaceIndex(
            @PathVariable String workspaceId,
            Principal principal) {
        
        return searchService.deleteWorkspaceIndex(workspaceId, principal.getName())
                .map(v -> ResponseEntity.ok(true))
                .onErrorResume(e -> {
                    log.error("Delete workspace index failed: workspace={}", workspaceId, e);
                    return Mono.just(ResponseEntity.badRequest().body(false));
                });
    }

    /**
     * 索引单个文档
     * POST /api/workspaces/:workspaceId/search/docs/:docId/index
     */
    @PostMapping("/docs/{docId}/index")
    public Mono<ResponseEntity<Boolean>> indexDocument(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            Principal principal) {
        
        return searchService.indexDocumentAdvanced(workspaceId, docId, principal.getName())
                .map(v -> ResponseEntity.ok(true))
                .onErrorResume(e -> {
                    log.error("Index document failed: workspace={}, doc={}", workspaceId, docId, e);
                    return Mono.just(ResponseEntity.badRequest().body(false));
                });
    }

    /**
     * 获取搜索历史
     * GET /api/search/history
     */
    @GetMapping("/history")
    public Mono<ResponseEntity<List<SearchHistoryDto>>> getSearchHistory(
            @RequestParam(value = "limit", defaultValue = "20") int limit,
            Principal principal) {
        
        return searchService.getSearchHistory(principal.getName(), limit)
                .map(history -> ResponseEntity.ok(history))
                .onErrorResume(e -> {
                    log.error("Get search history failed for user: {}", principal.getName(), e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }

    /**
     * 清除搜索历史
     * DELETE /api/search/history
     */
    @DeleteMapping("/history")
    public Mono<ResponseEntity<Boolean>> clearSearchHistory(Principal principal) {
        return searchService.clearSearchHistory(principal.getName())
                .map(v -> ResponseEntity.ok(true))
                .onErrorResume(e -> {
                    log.error("Clear search history failed for user: {}", principal.getName(), e);
                    return Mono.just(ResponseEntity.badRequest().body(false));
                });
    }

    /**
     * 搜索健康检查
     * GET /api/search/health
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<SearchHealthDto>> getSearchHealth() {
        return searchService.getSearchHealth()
                .map(health -> ResponseEntity.ok(health))
                .onErrorResume(e -> {
                    log.error("Search health check failed", e);
                    return Mono.just(ResponseEntity.badRequest().build());
                });
    }
}