package com.yunke.backend.system.controller;

import com.yunke.backend.security.AffineUserDetails;
import com.yunke.backend.system.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 搜索控制器
 */
@RestController("documentSearchController")
@RequestMapping("/api/search")
@RequiredArgsConstructor
@Slf4j
public class SearchController {

    private final SearchService searchService;

    /**
     * 全文搜索
     */
    @PostMapping
    public Mono<ResponseEntity<Map<String, Object>>> search(
            @RequestBody SearchRequest request,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        AffineUserDetails userDetails = (AffineUserDetails) authentication.getPrincipal();
        String userId = userDetails.getUserId();
        
        SearchService.SearchOptions options = new SearchService.SearchOptions(
                request.workspaceId(),
                userId,
                request.types(),
                request.from(),
                request.size(),
                request.sortBy(),
                request.sortOrder(),
                request.filters(),
                request.highlight()
        );
        
        return searchService.search(request.query(), options)
                .map(result -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("result", result);
                    response.put("query", request.query());
                    return ResponseEntity.ok(response);
                })
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Search failed")));
    }

    /**
     * 获取搜索建议
     */
    @GetMapping("/suggestions")
    public Mono<ResponseEntity<Map<String, Object>>> getSuggestions(
            @RequestParam String query,
            @RequestParam String workspaceId,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        return searchService.getSuggestions(query, workspaceId)
                .map(suggestions -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("suggestions", suggestions);
                    response.put("count", suggestions.size());
                    response.put("query", query);
                    return ResponseEntity.ok(response);
                })
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to get suggestions")));
    }

    /**
     * 重建搜索索引
     */
    @PostMapping("/reindex/{workspaceId}")
    public Mono<ResponseEntity<Map<String, Object>>> rebuildIndex(
            @PathVariable String workspaceId,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        return searchService.rebuildIndex(workspaceId)
                .then(Mono.fromCallable(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Search index rebuilt successfully");
                    response.put("workspaceId", workspaceId);
                    return ResponseEntity.ok(response);
                }))
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to rebuild index")));
    }

    /**
     * 索引文档
     */
    @PostMapping("/index")
    public Mono<ResponseEntity<Map<String, Object>>> indexDocument(
            @RequestBody IndexDocumentRequest request,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        return searchService.indexDocument(request.docId(), request.title(), request.content(), request.metadata())
                .then(Mono.fromCallable(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Document indexed successfully");
                    response.put("docId", request.docId());
                    return ResponseEntity.ok(response);
                }))
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to index document")));
    }

    /**
     * 删除文档索引
     */
    @DeleteMapping("/index/{docId}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteDocumentIndex(
            @PathVariable String docId,
            Authentication authentication) {
        
        if (authentication == null || !(authentication.getPrincipal() instanceof AffineUserDetails)) {
            return Mono.just(ResponseEntity.status(401).body(Map.of("error", "Unauthorized")));
        }
        
        return searchService.deleteDocument(docId)
                .then(Mono.fromCallable(() -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Document index deleted successfully");
                    response.put("docId", docId);
                    return ResponseEntity.ok(response);
                }))
                .onErrorReturn(ResponseEntity.badRequest().body(Map.of("error", "Failed to delete document index")));
    }

    // 请求数据类
    public record SearchRequest(
            String query,
            String workspaceId,
            List<String> types,
            int from,
            int size,
            String sortBy,
            String sortOrder,
            Map<String, Object> filters,
            boolean highlight
    ) {
        public SearchRequest {
            if (from < 0) from = 0;
            if (size <= 0 || size > 100) size = 20;
            if (sortBy == null) sortBy = "score";
            if (sortOrder == null) sortOrder = "desc";
            if (filters == null) filters = Map.of();
        }
    }
    
    public record IndexDocumentRequest(
            String docId,
            String title,
            String content,
            Map<String, Object> metadata
    ) {}
}