package com.yunke.backend.search.provider.impl;

import com.yunke.backend.search.dto.AggregateBucketDto;
import com.yunke.backend.search.dto.AggregateInputDto;
import com.yunke.backend.search.dto.AggregateResultDto;
import com.yunke.backend.search.dto.OperationOptionsDto;
import com.yunke.backend.system.dto.*;
import com.yunke.backend.search.enums.SearchProviderType;
import com.yunke.backend.search.enums.SearchQueryOccur;
import com.yunke.backend.search.enums.SearchQueryType;
import com.yunke.backend.search.enums.SearchTable;
import com.yunke.backend.search.provider.SearchProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Redis搜索提供者实现
 * 基于Redis进行简单的全文搜索功能
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisSearchProvider implements SearchProvider {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    
    private static final String INDEX_PREFIX = "search:index:";
    private static final String DOC_PREFIX = "search:doc:";
    private static final int MAX_RESULTS = 1000;

    @Override
    public SearchProviderType getType() {
        return SearchProviderType.REDIS;
    }

    @Override
    public Mono<Void> createTable(SearchTable table, String mapping) {
        // Redis不需要预先创建表结构
        log.info("Redis search table created: {}", table);
        return Mono.empty();
    }

    @Override
    public Mono<SearchResultDto> search(SearchTable table, SearchInputDto searchInput) {
        return performSearch(table, searchInput.getQuery(), searchInput.getOptions())
                .map(results -> {
                    SearchOptionsDto options = searchInput.getOptions();
                    SearchPaginationDto pagination = options.getPagination();
                    
                    int skip = pagination != null && pagination.getSkip() != null ? pagination.getSkip() : 0;
                    int limit = pagination != null && pagination.getLimit() != null ? pagination.getLimit() : 20;
                    
                    // 分页处理
                    List<SearchNodeDto> pagedResults = results.stream()
                            .skip(skip)
                            .limit(limit)
                            .collect(Collectors.toList());
                    
                    SearchResultPaginationDto paginationResult = SearchResultPaginationDto.builder()
                            .skip(skip)
                            .limit(limit)
                            .total((long) results.size())
                            .hasNext(skip + limit < results.size())
                            .build();
                    
                    return SearchResultDto.builder()
                            .nodes(pagedResults)
                            .pagination(paginationResult)
                            .build();
                });
    }

    @Override
    public Mono<AggregateResultDto> aggregate(SearchTable table, AggregateInputDto aggregateInput) {
        return performSearch(table, aggregateInput.getQuery(), null)
                .map(results -> {
                    // 简化的聚合实现：按字段分组
                    String field = aggregateInput.getField();
                    Map<String, List<SearchNodeDto>> groups = results.stream()
                            .collect(Collectors.groupingBy(node -> 
                                node.getFields().getOrDefault(field, "unknown").toString()));
                    
                    List<AggregateBucketDto> buckets = groups.entrySet().stream()
                            .map(entry -> AggregateBucketDto.builder()
                                    .key(entry.getKey())
                                    .docCount((long) entry.getValue().size())
                                    .hits(entry.getValue())
                                    .build())
                            .collect(Collectors.toList());
                    
                    return AggregateResultDto.builder()
                            .buckets(buckets)
                            .totalDocs((long) results.size())
                            .build();
                });
    }

    @Override
    public Mono<Void> write(SearchTable table, List<Map<String, Object>> documents, OperationOptionsDto options) {
        return Flux.fromIterable(documents)
                .flatMap(doc -> indexDocument(table, doc))
                .then();
    }

    @Override
    public Mono<Void> deleteByQuery(SearchTable table, Map<String, Object> query, OperationOptionsDto options) {
        String pattern = getDocumentKey(table, "*");
        return redisTemplate.keys(pattern)
                .flatMap(key -> redisTemplate.opsForValue().get(key)
                        .cast(Map.class)
                        .filter(doc -> matchesQuery(doc, query))
                        .flatMap(doc -> redisTemplate.delete(key)))
                .then();
    }

    @Override
    public Mono<Void> deleteDocument(SearchTable table, String documentId) {
        String key = getDocumentKey(table, documentId);
        return redisTemplate.delete(key).then();
    }

    @Override
    public Mono<Boolean> tableExists(SearchTable table) {
        // Redis中总是认为表存在
        return Mono.just(true);
    }

    @Override
    public Mono<Void> deleteTable(SearchTable table) {
        String pattern = getDocumentKey(table, "*");
        return redisTemplate.keys(pattern)
                .flatMap(redisTemplate::delete)
                .then();
    }

    @Override
    public Mono<Long> getDocumentCount(SearchTable table) {
        String pattern = getDocumentKey(table, "*");
        return redisTemplate.keys(pattern)
                .count();
    }

    @Override
    public Mono<Void> refresh(SearchTable table) {
        // Redis不需要刷新操作
        return Mono.empty();
    }

    @Override
    public Mono<Boolean> isHealthy() {
        // 使用简单的存在性检查代替ping操作
        return redisTemplate.hasKey("health:check")
                .defaultIfEmpty(true)  // 即使key不存在也视为健康
                .onErrorReturn(false);
    }

    // ==================== 私有方法 ====================

    private Mono<List<SearchNodeDto>> performSearch(SearchTable table, SearchQueryDto query, SearchOptionsDto options) {
        String pattern = getDocumentKey(table, "*");
        
        return redisTemplate.keys(pattern)
                .flatMap(key -> redisTemplate.opsForValue().get(key)
                        .cast(Map.class)
                        .map(doc -> {
                            Map<String, Object> docMap = (Map<String, Object>) doc;
                            return SearchNodeDto.builder()
                                    .fields(docMap)
                                    .score(calculateScore(docMap, query))
                                    .build();
                        }))
                .filter(node -> matchesQuery(node.getFields(), query))
                .sort((node1, node2) -> Double.compare(node2.getScore(), node1.getScore()))
                .take(MAX_RESULTS)
                .collectList();
    }

    private Mono<Void> indexDocument(SearchTable table, Map<String, Object> document) {
        String documentId = extractDocumentId(document);
        String key = getDocumentKey(table, documentId);
        
        return redisTemplate.opsForValue()
                .set(key, document)
                .then(indexKeywords(table, documentId, document));
    }

    private Mono<Void> indexKeywords(SearchTable table, String documentId, Map<String, Object> document) {
        // 提取可搜索的关键词
        Set<String> keywords = extractKeywords(document);
        
        return Flux.fromIterable(keywords)
                .flatMap(keyword -> {
                    String indexKey = getIndexKey(table, keyword);
                    return redisTemplate.opsForSet().add(indexKey, documentId);
                })
                .then();
    }

    private boolean matchesQuery(Map<String, Object> document, SearchQueryDto query) {
        if (query == null) {
            return true;
        }
        
        return switch (query.getType()) {
            case MATCH -> matchesField(document, query.getField(), query.getMatch());
            case BOOLEAN -> matchesBooleanQuery(document, query);
            case EXISTS -> document.containsKey(query.getField());
            case ALL -> true;
            default -> false;
        };
    }

    private boolean matchesQuery(Map<String, Object> document, Map<String, Object> query) {
        // 简化的查询匹配实现
        return query.entrySet().stream()
                .allMatch(entry -> {
                    Object docValue = document.get(entry.getKey());
                    return docValue != null && docValue.toString().contains(entry.getValue().toString());
                });
    }

    private boolean matchesField(Map<String, Object> document, String field, String value) {
        if (field == null || value == null) {
            return false;
        }
        
        Object fieldValue = document.get(field);
        if (fieldValue == null) {
            return false;
        }
        
        return fieldValue.toString().toLowerCase().contains(value.toLowerCase());
    }

    private boolean matchesBooleanQuery(Map<String, Object> document, SearchQueryDto query) {
        if (query.getQueries() == null || query.getQueries().isEmpty()) {
            return true;
        }
        
        return switch (query.getOccur()) {
            case MUST -> query.getQueries().stream().allMatch(q -> matchesQuery(document, q));
            case SHOULD -> query.getQueries().stream().anyMatch(q -> matchesQuery(document, q));
            case MUST_NOT -> query.getQueries().stream().noneMatch(q -> matchesQuery(document, q));
        };
    }

    private double calculateScore(Map<String, Object> document, SearchQueryDto query) {
        // 简化的评分算法
        if (query == null || query.getType() != SearchQueryType.MATCH) {
            return 1.0;
        }
        
        String field = query.getField();
        String match = query.getMatch();
        
        if (field == null || match == null) {
            return 1.0;
        }
        
        Object fieldValue = document.get(field);
        if (fieldValue == null) {
            return 0.0;
        }
        
        String text = fieldValue.toString().toLowerCase();
        String searchTerm = match.toLowerCase();
        
        // 计算匹配度
        double score = 0.0;
        if (text.contains(searchTerm)) {
            score = 1.0;
            // 完全匹配给更高分
            if (text.equals(searchTerm)) {
                score = 2.0;
            }
            // 开头匹配给较高分
            else if (text.startsWith(searchTerm)) {
                score = 1.5;
            }
        }
        
        // 应用boost
        if (query.getBoost() != null) {
            score *= query.getBoost();
        }
        
        return score;
    }

    private Set<String> extractKeywords(Map<String, Object> document) {
        Set<String> keywords = new HashSet<>();
        
        document.values().forEach(value -> {
            if (value instanceof String) {
                String text = (String) value;
                // 简单的分词：按空格和标点符号分割
                String[] words = text.toLowerCase()
                        .replaceAll("[^\\w\\s]", " ")
                        .split("\\s+");
                
                for (String word : words) {
                    if (word.length() > 2) { // 忽略过短的词
                        keywords.add(word);
                    }
                }
            }
        });
        
        return keywords;
    }

    private String extractDocumentId(Map<String, Object> document) {
        // 尝试从文档中提取ID
        Object id = document.get("id");
        if (id != null) {
            return id.toString();
        }
        
        Object docId = document.get("docId");
        if (docId != null) {
            return docId.toString();
        }
        
        Object blockId = document.get("blockId");
        if (blockId != null) {
            return blockId.toString();
        }
        
        // 如果没有找到ID，生成一个
        return UUID.randomUUID().toString();
    }

    private String getDocumentKey(SearchTable table, String documentId) {
        return DOC_PREFIX + table.getValue() + ":" + documentId;
    }

    private String getIndexKey(SearchTable table, String keyword) {
        return INDEX_PREFIX + table.getValue() + ":" + keyword;
    }
}

