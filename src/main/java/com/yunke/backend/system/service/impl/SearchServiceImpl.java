package com.yunke.backend.system.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yunke.backend.system.dto.*;
import com.yunke.backend.search.enums.SearchProviderType;
import com.yunke.backend.search.enums.SearchTable;
import com.yunke.backend.search.provider.SearchProvider;
import com.yunke.backend.security.service.PermissionService;
import com.yunke.backend.system.service.SearchService;
import com.yunke.backend.workspace.domain.entity.WorkspaceDoc;
import com.yunke.backend.workspace.service.WorkspaceDocService;
import com.yunke.backend.common.dto.PaginatedResponse;
import com.yunke.backend.common.dto.PaginationInput;
import com.yunke.backend.search.dto.AggregateInputDto;
import com.yunke.backend.search.dto.AggregateResultDto;
import com.yunke.backend.search.dto.OperationOptionsDto;
import com.yunke.backend.search.enums.SearchQueryType;
import com.yunke.backend.search.enums.SearchQueryOccur;
import com.yunke.backend.common.exception.PermissionDeniedException;
import com.yunke.backend.common.exception.BusinessException;
import com.yunke.backend.security.constants.PermissionActions;
import com.yunke.backend.security.util.PermissionUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * 搜索服务实现
 * 基于Redis的简化全文搜索
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SearchServiceImpl implements SearchService {

    private final ReactiveRedisTemplate<String, Object> reactiveRedisTemplate;
    private final WorkspaceDocService docService;
    private final PermissionService permissionService;
    private final ObjectMapper objectMapper;
    private final SearchProvider searchProvider;

    private static final String INDEX_PREFIX = "search_index:";
    private static final String DOC_PREFIX = "search_doc:";
    private static final String WORKSPACE_INDEX_PREFIX = "workspace_index:";

    @Override
    public Mono<PaginatedResponse<com.yunke.backend.document.dto.DocDto>> searchDocs(String workspaceId, String query, PaginationInput pagination, String userId) {
        return Mono.fromCallable(() -> {
            // Simple fallback to database search for now
            SearchOptions options = new SearchOptions(
                    workspaceId, userId, List.of("document"), 
                    pagination.getOffset(), pagination.getFirst(),
                    "score", "desc", Map.of(), true
            );
            
            SearchResult result = search(query, options).block();
            
            List<com.yunke.backend.document.dto.DocDto> docs = result.hits().stream()
                    .map(hit -> com.yunke.backend.document.dto.DocDto.builder()
                            .id(hit.id())
                            .title(hit.title())
                            .summary(hit.content())
                            .workspaceId(workspaceId)
                            .build())
                    .toList();
            
            return PaginatedResponse.of(
                    docs, 
                    result.totalHits(),
                    pagination.getPage(),
                    pagination.getSize()
            );
        });
    }

    @Override
    public Mono<SearchResult> search(String query, SearchOptions options) {
        log.debug("Searching: query={}, workspace={}", query, options.workspaceId());
        
        return Mono.fromCallable(() -> {
            // 检查权限
            if (!permissionService.hasWorkspaceAccess(options.userId(), options.workspaceId())) {
                throw new PermissionDeniedException(options.userId(), "workspace", "read");
            }
            return query;
        })
        .flatMap(q -> performSearch(q, options))
        .doOnSuccess(result -> log.debug("Search completed: {} hits", result.totalHits()))
        .doOnError(error -> log.error("Search failed", error));
    }

    @Override
    public Mono<Void> indexDocument(String docId, String title, String content, Map<String, Object> metadata) {
        log.debug("Indexing document: {}", docId);
        
        return Mono.fromCallable(() -> {
            try {
                // 创建搜索文档
                SearchDocument searchDoc = new SearchDocument(
                        docId,
                        title,
                        content,
                        "document",
                        metadata,
                        Instant.now()
                );
                
                // 序列化
                String docJson = objectMapper.writeValueAsString(searchDoc);
                String docKey = DOC_PREFIX + docId;
                
                // 添加到工作空间索引
                String workspaceId = (String) metadata.get("workspaceId");
                String workspaceIndexKey = workspaceId != null ? WORKSPACE_INDEX_PREFIX + workspaceId : null;
                
                return new IndexData(docKey, docJson, workspaceIndexKey, docId, title, content);
            } catch (Exception e) {
                throw new BusinessException("Failed to prepare index data", e);
            }
        })
        .flatMap(data -> {
            // 组合所有Redis操作
            Mono<Void> saveDocMono = reactiveRedisTemplate.opsForValue()
                .set(data.docKey, data.docJson, Duration.ofDays(365))
                .then();
            
            Mono<Void> addToWorkspaceMono = data.workspaceIndexKey != null
                ? reactiveRedisTemplate.opsForSet().add(data.workspaceIndexKey, data.docId).then()
                : Mono.empty();
            
            Mono<Void> createKeywordMono = createKeywordIndex(data.docId, data.title + " " + data.content);
            
            return Mono.when(saveDocMono, addToWorkspaceMono, createKeywordMono)
                .thenReturn((Void) null);
        })
        .doOnSuccess(v -> log.debug("Document indexed successfully: {}", docId))
        .doOnError(error -> log.error("Failed to index document: {}", docId, error));
    }

    @Override
    public Mono<Void> deleteDocument(String docId) {
        log.debug("Deleting document index: {}", docId);
        
        return Mono.fromCallable(() -> {
            String docKey = DOC_PREFIX + docId;
            return docKey;
        })
        .flatMap(docKey -> 
            Mono.when(
                reactiveRedisTemplate.delete(docKey),
                deleteKeywordIndex(docId)
            )
            .thenReturn((Void) null)
        )
        .doOnSuccess(v -> log.debug("Document index deleted successfully: {}", docId))
        .doOnError(error -> log.error("Failed to delete document index: {}", docId, error));
    }

    @Override
    public Mono<Void> updateDocument(String docId, String title, String content, Map<String, Object> metadata) {
        log.debug("Updating document index: {}", docId);
        
        return deleteDocument(docId)
                .then(indexDocument(docId, title, content, metadata));
    }

    @Override
    public Mono<Void> rebuildIndex(String workspaceId) {
        log.info("Rebuilding search index for workspace: {}", workspaceId);
        
        return Mono.fromCallable(() -> {
            // 获取工作空间的所有文档
            return docService.getWorkspaceDocs(workspaceId);
        })
        .flatMapMany(Flux::fromIterable)
        .flatMap(doc -> {
            Map<String, Object> metadata = Map.of(
                    "workspaceId", workspaceId,
                    "updatedAt", doc.getUpdatedAt(),
                    "public", doc.getPublic() != null ? doc.getPublic() : false
            );
            return indexDocument(doc.getId(), doc.getTitle(), "", metadata);
        })
        .then(
            reactiveRedisTemplate.delete(WORKSPACE_INDEX_PREFIX + workspaceId)
                .then()
        )
        .doOnSuccess(v -> log.info("Search index rebuilt successfully for workspace: {}", workspaceId))
        .doOnError(error -> log.error("Failed to rebuild search index for workspace: {}", workspaceId, error));
    }

    @Override
    public Mono<List<String>> getSuggestions(String query, String workspaceId) {
        log.debug("Getting search suggestions: query={}, workspace={}", query, workspaceId);
        
        return Mono.fromCallable(() -> {
            // 简化的建议实现 - 基于关键词匹配
            String workspaceIndexKey = WORKSPACE_INDEX_PREFIX + workspaceId;
            
            return reactiveRedisTemplate.opsForSet().members(workspaceIndexKey)
                    .cast(String.class)
                    .flatMap(docId -> {
                        String docKey = DOC_PREFIX + docId;
                        return reactiveRedisTemplate.opsForValue().get(docKey);
                    })
                    .cast(String.class)
                    .flatMap(docJson -> {
                        try {
                            SearchDocument doc = objectMapper.readValue(docJson, SearchDocument.class);
                            return Mono.just(doc);
                        } catch (Exception e) {
                            return Mono.empty();
                        }
                    })
                    .filter(doc -> doc.title().toLowerCase().contains(query.toLowerCase()))
                    .map(SearchDocument::title)
                    .distinct()
                    .take(10)
                    .collectList()
                    .block();
        })
        .doOnSuccess(suggestions -> log.debug("Generated {} suggestions", suggestions.size()))
        .doOnError(error -> log.error("Failed to get suggestions", error));
    }

    /**
     * 执行搜索
     */
    private Mono<SearchResult> performSearch(String query, SearchOptions options) {
        return Mono.fromCallable(() -> {
            long startTime = System.currentTimeMillis();
            
            List<String> docIds = getWorkspaceDocIds(options.workspaceId());
            if (docIds == null || docIds.isEmpty()) {
                return new SearchResult(List.of(), 0, 0.0, System.currentTimeMillis() - startTime, Map.of());
            }
            
            List<SearchHit> hits = searchMatchingDocuments(query, docIds);
            List<SearchHit> sortedHits = sortAndPageHits(hits, options);
            
            long took = System.currentTimeMillis() - startTime;
            double maxScore = hits.stream().mapToDouble(SearchHit::score).max().orElse(0.0);
            
            return new SearchResult(sortedHits, hits.size(), maxScore, took, Map.of());
        });
    }
    
    /**
     * 获取工作空间的文档ID列表
     */
    private List<String> getWorkspaceDocIds(String workspaceId) {
        String workspaceIndexKey = WORKSPACE_INDEX_PREFIX + workspaceId;
        return reactiveRedisTemplate.opsForSet().members(workspaceIndexKey)
                .cast(String.class)
                .collectList()
                .block();
    }
    
    /**
     * 搜索匹配的文档
     */
    private List<SearchHit> searchMatchingDocuments(String query, List<String> docIds) {
        List<SearchHit> hits = new ArrayList<>();
        
        for (String docId : docIds) {
            SearchHit hit = createSearchHitIfMatches(query, docId);
            if (hit != null) {
                hits.add(hit);
            }
        }
        
        return hits;
    }
    
    /**
     * 如果文档匹配查询，则创建 SearchHit
     */
    private SearchHit createSearchHitIfMatches(String query, String docId) {
        String docKey = DOC_PREFIX + docId;
        Object docJson = reactiveRedisTemplate.opsForValue().get(docKey).block();
        
        if (docJson == null) {
            return null;
        }
        
        try {
            SearchDocument doc = objectMapper.readValue(docJson.toString(), SearchDocument.class);
            double score = calculateScore(query, doc);
            
            if (score > 0) {
                Map<String, List<String>> highlights = generateHighlights(query, doc);
                String summary = doc.content().length() > 200 ? 
                        doc.content().substring(0, 200) + "..." : doc.content();
                
                return new SearchHit(
                        doc.id(),
                        doc.title(),
                        summary,
                        doc.type(),
                        score,
                        doc.metadata(),
                        highlights
                );
            }
        } catch (Exception e) {
            log.warn("Failed to parse search document: {}", docId, e);
        }
        
        return null;
    }
    
    /**
     * 排序和分页搜索结果
     */
    private List<SearchHit> sortAndPageHits(List<SearchHit> hits, SearchOptions options) {
        // 排序结果
        hits.sort((a, b) -> Double.compare(b.score(), a.score()));
        
        // 分页
        int from = options.from();
        int size = options.size();
        return hits.stream()
                .skip(from)
                .limit(size)
                .collect(Collectors.toList());
    }

    /**
     * 计算搜索评分
     */
    private double calculateScore(String query, SearchDocument doc) {
        String lowerQuery = query.toLowerCase();
        String lowerTitle = doc.title().toLowerCase();
        String lowerContent = doc.content().toLowerCase();
        
        double score = 0.0;
        
        // 标题匹配权重更高
        if (lowerTitle.contains(lowerQuery)) {
            score += 2.0;
        }
        
        // 内容匹配
        if (lowerContent.contains(lowerQuery)) {
            score += 1.0;
        }
        
        // 关键词匹配
        String[] queryWords = lowerQuery.split("\\s+");
        for (String word : queryWords) {
            if (lowerTitle.contains(word)) {
                score += 0.5;
            }
            if (lowerContent.contains(word)) {
                score += 0.2;
            }
        }
        
        return score;
    }

    /**
     * 生成高亮
     */
    private Map<String, List<String>> generateHighlights(String query, SearchDocument doc) {
        Map<String, List<String>> highlights = new HashMap<>();
        
        String lowerQuery = query.toLowerCase();
        
        // 标题高亮
        if (doc.title().toLowerCase().contains(lowerQuery)) {
            String highlightedTitle = doc.title().replaceAll(
                    "(?i)" + query, "<mark>" + query + "</mark>"
            );
            highlights.put("title", List.of(highlightedTitle));
        }
        
        // 内容高亮
        if (doc.content().toLowerCase().contains(lowerQuery)) {
            String highlightedContent = doc.content().replaceAll(
                    "(?i)" + query, "<mark>" + query + "</mark>"
            );
            // 截取包含关键词的片段
            String snippet = extractSnippet(highlightedContent, query, 200);
            highlights.put("content", List.of(snippet));
        }
        
        return highlights;
    }

    /**
     * 提取包含关键词的片段
     */
    private String extractSnippet(String content, String query, int maxLength) {
        int queryIndex = content.toLowerCase().indexOf(query.toLowerCase());
        if (queryIndex == -1) {
            return content.length() > maxLength ? content.substring(0, maxLength) + "..." : content;
        }
        
        int start = Math.max(0, queryIndex - maxLength / 2);
        int end = Math.min(content.length(), start + maxLength);
        
        String snippet = content.substring(start, end);
        if (start > 0) {
            snippet = "..." + snippet;
        }
        if (end < content.length()) {
            snippet = snippet + "...";
        }
        
        return snippet;
    }

    /**
     * 创建关键词索引
     */
    private Mono<Void> createKeywordIndex(String docId, String text) {
        return Mono.fromCallable(() -> {
            String[] words = text.toLowerCase().split("\\W+");
            return Arrays.stream(words)
                .filter(word -> word.length() > 2)
                .map(word -> INDEX_PREFIX + word)
                .collect(java.util.stream.Collectors.toList());
        })
        .flatMapMany(Flux::fromIterable)
        .flatMap(indexKey -> 
            Mono.when(
                reactiveRedisTemplate.opsForSet().add(indexKey, docId),
                reactiveRedisTemplate.expire(indexKey, Duration.ofDays(365))
            )
        )
        .then();
    }

    /**
     * 删除关键词索引
     */
    private Mono<Void> deleteKeywordIndex(String docId) {
        return Mono.fromCallable(() -> {
            // 简化实现 - 实际应该遍历所有关键词索引
            return null;
        }).then();
    }

    // ==================== 新增高级搜索方法实现 ====================

    @Override
    public Mono<SearchResultDto> advancedSearch(String workspaceId, SearchInputDto searchInput, String userId) {
        return PermissionUtils.requireWorkspacePermission(permissionService, workspaceId, userId, 
                PermissionActions.WORKSPACE_READ,
                () -> {
                    // 添加工作空间过滤条件
                    addWorkspaceFilter(searchInput, workspaceId);
                    return searchProvider.search(searchInput.getTable(), searchInput);
                })
                .doOnSuccess(result -> 
                    addSearchHistory(userId, extractQueryText(searchInput.getQuery()), workspaceId)
                        .subscribeOn(Schedulers.boundedElastic())
                        .doOnError(e -> log.warn("Failed to add search history", e))
                        .subscribe()
                );
    }

    @Override
    public Mono<AggregateResultDto> aggregate(String workspaceId, AggregateInputDto aggregateInput, String userId) {
        return PermissionUtils.requireWorkspacePermission(permissionService, workspaceId, userId, 
                PermissionActions.WORKSPACE_READ,
                () -> {
                    // 添加工作空间过滤条件
                    addWorkspaceFilterToAggregate(aggregateInput, workspaceId);
                    return searchProvider.aggregate(aggregateInput.getTable(), aggregateInput);
                });
    }

    @Override
    public Mono<IndexStatsDto> indexWorkspace(String workspaceId, String userId) {
        return PermissionUtils.requireWorkspacePermission(permissionService, workspaceId, userId, 
                "Workspace.ManageIndex",
                () -> Mono.fromCallable(() -> {
                    log.info("Starting workspace indexing: {}", workspaceId);
                    
                    // 获取工作空间的所有文档
                    List<WorkspaceDoc> docs = docService.getWorkspaceDocs(workspaceId);
                    
                    // 索引文档
                    List<Map<String, Object>> docMaps = docs.stream()
                            .map(doc -> convertDocToSearchDocument(doc, workspaceId))
                            .collect(Collectors.toList());
                    
                    OperationOptionsDto options = OperationOptionsDto.builder()
                            .refresh(true)
                            .build();
                    
                    searchProvider.write(SearchTable.DOC, docMaps, options).block();
                    
                    return IndexStatsDto.builder()
                            .workspaceId(workspaceId)
                            .totalDocs((long) docs.size())
                            .indexedDocs((long) docs.size())
                            .lastIndexed(LocalDateTime.now())
                            .isIndexing(false)
                            .status("completed")
                            .build();
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    public Mono<Void> indexDocumentAdvanced(String workspaceId, String docId, String userId) {
        return PermissionUtils.requireDocPermission(permissionService, workspaceId, docId, userId, 
                PermissionActions.DOC_READ,
                () -> Mono.fromCallable(() -> {
                    // 获取文档详细信息 - simplified implementation
                    WorkspaceDoc doc = new WorkspaceDoc();
                    doc.setWorkspaceId(workspaceId);
                    doc.setId(docId);
                    doc.setTitle("Document " + docId);
                    Map<String, Object> docMap = convertDocToSearchDocument(doc, workspaceId);
                    
                    OperationOptionsDto options = OperationOptionsDto.builder()
                            .refresh(true)
                            .build();
                    
                    searchProvider.write(SearchTable.DOC, List.of(docMap), options).block();
                    
                    log.info("Document indexed: {}/{}", workspaceId, docId);
                    return null;
                })
                .subscribeOn(Schedulers.boundedElastic()))
                .then();
    }

    @Override
    public Mono<Void> deleteWorkspaceIndex(String workspaceId, String userId) {
        return PermissionUtils.requireWorkspacePermission(permissionService, workspaceId, userId, 
                "Workspace.ManageIndex",
                () -> {
                    Map<String, Object> query = Map.of("workspace_id", workspaceId);
                    OperationOptionsDto options = OperationOptionsDto.builder()
                            .refresh(true)
                            .build();
                    
                    return searchProvider.deleteByQuery(SearchTable.DOC, query, options)
                            .then(searchProvider.deleteByQuery(SearchTable.BLOCK, query, options));
                });
    }

    @Override
    public Mono<IndexStatsDto> getIndexStats(String workspaceId, String userId) {
        return PermissionUtils.requireWorkspacePermission(permissionService, workspaceId, userId, 
                PermissionActions.WORKSPACE_READ,
                () -> Mono.fromCallable(() -> {
                    // 简化实现 - 实际应该查询搜索引擎的统计信息
                    long docCount = searchProvider.getDocumentCount(SearchTable.DOC).block();
                    
                    return IndexStatsDto.builder()
                            .workspaceId(workspaceId)
                            .totalDocs(docCount)
                            .indexedDocs(docCount)
                            .lastIndexed(LocalDateTime.now())
                            .isIndexing(false)
                            .status("healthy")
                            .build();
                })
                .subscribeOn(Schedulers.boundedElastic()));
    }

    @Override
    public Mono<SearchResultDto> searchBlocks(String workspaceId, SearchInputDto searchInput, String userId) {
        return PermissionUtils.requireWorkspacePermission(permissionService, workspaceId, userId, 
                PermissionActions.WORKSPACE_READ,
                () -> {
                    // 设置搜索表为BLOCK
                    searchInput.setTable(SearchTable.BLOCK);
                    addWorkspaceFilter(searchInput, workspaceId);
                    return searchProvider.search(SearchTable.BLOCK, searchInput);
                });
    }

    @Override
    public Mono<List<SearchSuggestionDto>> autoComplete(String workspaceId, String query, int limit, String userId) {
        return PermissionUtils.requireWorkspacePermission(permissionService, workspaceId, userId, 
                PermissionActions.WORKSPACE_READ,
                () -> getSuggestions(query, workspaceId)
                        .map(suggestions -> suggestions.stream()
                                .limit(limit)
                                .map(text -> SearchSuggestionDto.builder()
                                        .text(text)
                                        .type("document")
                                        .score(1.0)
                                            .workspaceId(workspaceId)
                                            .build())
                                    .collect(Collectors.toList()))
                );
    }

    @Override
    public Mono<List<SearchHistoryDto>> getSearchHistory(String userId, int limit) {
        String historyKey = "search_history:" + userId;
        return reactiveRedisTemplate.opsForList().range(historyKey, 0, limit - 1)
                .cast(String.class)
                .flatMap(historyJson -> {
                    try {
                        SearchHistoryDto history = objectMapper.readValue(historyJson, SearchHistoryDto.class);
                        return Mono.just(history);
                    } catch (Exception e) {
                        return Mono.empty();
                    }
                })
                .collectList();
    }

    @Override
    public Mono<Void> addSearchHistory(String userId, String query, String workspaceId) {
        if (query == null || query.trim().isEmpty()) {
            return Mono.empty();
        }
        
        return Mono.fromCallable(() -> {
            try {
                SearchHistoryDto history = SearchHistoryDto.builder()
                        .id(UUID.randomUUID().toString())
                        .userId(userId)
                        .query(query.trim())
                        .workspaceId(workspaceId)
                        .searchedAt(LocalDateTime.now())
                        .build();
                
                String historyJson = objectMapper.writeValueAsString(history);
                String historyKey = "search_history:" + userId;
                
                return new HistoryData(historyKey, historyJson);
            } catch (Exception e) {
                throw new RuntimeException("Failed to prepare search history", e);
            }
        })
        .flatMap(data -> 
            Mono.when(
                reactiveRedisTemplate.opsForList().leftPush(data.historyKey, data.historyJson),
                reactiveRedisTemplate.opsForList().trim(data.historyKey, 0, 49),
                reactiveRedisTemplate.expire(data.historyKey, Duration.ofDays(30))
            )
            .thenReturn((Void) null)
        )
        .onErrorResume(e -> {
            log.warn("Failed to add search history", e);
            return Mono.empty();
        });
    }

    @Override
    public Mono<Void> clearSearchHistory(String userId) {
        String historyKey = "search_history:" + userId;
        return reactiveRedisTemplate.delete(historyKey).then();
    }

    @Override
    public Mono<SearchHealthDto> getSearchHealth() {
        return searchProvider.isHealthy()
                .map(isHealthy -> SearchHealthDto.builder()
                        .isHealthy(isHealthy)
                        .providerType(searchProvider.getType())
                        .status(isHealthy ? "healthy" : "unhealthy")
                        .lastChecked(LocalDateTime.now())
                        .responseTime(System.currentTimeMillis()) // 简化实现
                        .build())
                .onErrorReturn(SearchHealthDto.builder()
                        .isHealthy(false)
                        .providerType(searchProvider.getType())
                        .status("error")
                        .lastChecked(LocalDateTime.now())
                        .errorMessage("Health check failed")
                        .build());
    }

    // ==================== 辅助方法 ====================

    private void addWorkspaceFilter(SearchInputDto searchInput, String workspaceId) {
        // 简化实现：在查询中添加工作空间过滤条件
        SearchQueryDto workspaceFilter = SearchQueryDto.builder()
                .type(SearchQueryType.MATCH)
                .field("workspace_id")
                .match(workspaceId)
                .build();
        
        // 如果已有查询，组合为布尔查询
        SearchQueryDto currentQuery = searchInput.getQuery();
        if (currentQuery != null) {
            SearchQueryDto combinedQuery = SearchQueryDto.builder()
                    .type(SearchQueryType.BOOLEAN)
                    .occur(SearchQueryOccur.MUST)
                    .queries(List.of(currentQuery, workspaceFilter))
                    .build();
            searchInput.setQuery(combinedQuery);
        } else {
            searchInput.setQuery(workspaceFilter);
        }
    }

    private void addWorkspaceFilterToAggregate(AggregateInputDto aggregateInput, String workspaceId) {
        SearchQueryDto workspaceFilter = SearchQueryDto.builder()
                .type(SearchQueryType.MATCH)
                .field("workspace_id")
                .match(workspaceId)
                .build();
        
        SearchQueryDto currentQuery = aggregateInput.getQuery();
        if (currentQuery != null) {
            SearchQueryDto combinedQuery = SearchQueryDto.builder()
                    .type(SearchQueryType.BOOLEAN)
                    .occur(SearchQueryOccur.MUST)
                    .queries(List.of(currentQuery, workspaceFilter))
                    .build();
            aggregateInput.setQuery(combinedQuery);
        } else {
            aggregateInput.setQuery(workspaceFilter);
        }
    }

    private String extractQueryText(SearchQueryDto query) {
        if (query == null) {
            return "";
        }
        
        if (query.getMatch() != null) {
            return query.getMatch();
        }
        
        if (query.getQueries() != null && !query.getQueries().isEmpty()) {
            return query.getQueries().stream()
                    .map(this::extractQueryText)
                    .filter(text -> !text.isEmpty())
                    .collect(Collectors.joining(" "));
        }
        
        return "";
    }

    private Map<String, Object> convertDocToSearchDocument(WorkspaceDoc doc, String workspaceId) {
        Map<String, Object> docMap = new HashMap<>();
        docMap.put("id", doc.getId());
        docMap.put("doc_id", doc.getId());
        docMap.put("workspace_id", workspaceId);
        docMap.put("title", doc.getTitle() != null ? doc.getTitle() : "");
        docMap.put("summary", doc.getSummary() != null ? doc.getSummary() : "");
        docMap.put("created_at", doc.getCreatedAt());
        docMap.put("updated_at", doc.getUpdatedAt());
        docMap.put("is_public", doc.getPublic() != null ? doc.getPublic() : false);
        docMap.put("mode", doc.getMode() != null ? doc.getMode() : 0);
        return docMap;
    }

    /**
     * 搜索文档内部类
     */
    private record SearchDocument(
            String id,
            String title,
            String content,
            String type,
            Map<String, Object> metadata,
            Instant indexedAt
    ) {}
    
    /**
     * 索引数据内部类
     */
    private record IndexData(
            String docKey,
            String docJson,
            String workspaceIndexKey,
            String docId,
            String title,
            String content
    ) {}
    
    /**
     * 搜索历史数据内部类
     */
    private record HistoryData(
            String historyKey,
            String historyJson
    ) {}
}