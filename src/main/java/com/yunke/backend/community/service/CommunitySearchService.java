package com.yunke.backend.community.service;

import com.yunke.backend.search.dto.SearchRequest;
import com.yunke.backend.search.dto.SearchSuggestionResponse;
import com.yunke.backend.community.domain.entity.CommunityDocument;
import com.yunke.backend.system.domain.entity.SearchLog;
import com.yunke.backend.community.repository.CommunityDocumentRepository;
import com.yunke.backend.system.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.persistence.criteria.Predicate;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 搜索服务类
 * 负责文档搜索、搜索建议、热门关键词等功能
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CommunitySearchService {

    private final CommunityDocumentRepository documentRepository;
    private final SearchLogRepository searchLogRepository;

    /**
     * 综合搜索文档
     */
    public Page<CommunityDocument> searchDocuments(SearchRequest request) {
        log.info("开始搜索文档，参数: {}", request);

        // 记录搜索日志
        recordSearchLog(request);

        // 构建查询条件
        Specification<CommunityDocument> spec = buildSearchSpecification(request);
        
        // 构建分页和排序
        Pageable pageable = buildPageable(request);
        
        // 执行查询
        Page<CommunityDocument> result = documentRepository.findAll(spec, pageable);

        // 记录搜索结果数量
        updateSearchResultCount(request, (int) result.getTotalElements());

        log.info("搜索完成，关键词: {}, 结果数量: {}", request.getKeyword(), result.getTotalElements());
        return result;
    }

    /**
     * 构建搜索规格
     */
    private Specification<CommunityDocument> buildSearchSpecification(SearchRequest request) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            // 只显示公开文档
            predicates.add(criteriaBuilder.isTrue(root.get("isPublic")));
            
            // 关键词搜索
            if (StringUtils.hasText(request.getKeyword())) {
                String keyword = "%" + request.getKeyword().toLowerCase() + "%";
                Predicate titlePredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("title")), keyword);
                Predicate descPredicate = criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("description")), keyword);
                predicates.add(criteriaBuilder.or(titlePredicate, descPredicate));
            }
            
            // 分类过滤
            if (request.getCategoryId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoryId"), request.getCategoryId()));
            }
            
            // 付费类型过滤
            if (Boolean.TRUE.equals(request.getPaidOnly())) {
                predicates.add(criteriaBuilder.isTrue(root.get("isPaid")));
            }
            if (Boolean.TRUE.equals(request.getFreeOnly())) {
                predicates.add(criteriaBuilder.isFalse(root.get("isPaid")));
            }
            
            // 价格范围过滤
            if (request.getMinPrice() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                    root.get("price"), BigDecimal.valueOf(request.getMinPrice())));
            }
            if (request.getMaxPrice() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                    root.get("price"), BigDecimal.valueOf(request.getMaxPrice())));
            }
            
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
    
    /**
     * 构建分页和排序
     */
    private Pageable buildPageable(SearchRequest request) {
        // 构建排序
        Sort sort = buildSort(request);
        
        // 构建分页
        int page = Math.max(0, request.getPage() - 1); // JPA分页从0开始
        int size = Math.max(1, Math.min(100, request.getSize())); // 限制页面大小
        
        return PageRequest.of(page, size, sort);
    }
    
    /**
     * 构建排序
     */
    private Sort buildSort(SearchRequest request) {
        String sortBy = request.getSortBy();
        String direction = request.getSortDirection();
        
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction) ? 
            Sort.Direction.ASC : Sort.Direction.DESC;
            
        switch (sortBy) {
            case "view_count":
                return Sort.by(sortDirection, "viewCount");
            case "like_count":
                return Sort.by(sortDirection, "likeCount");
            case "created_at":
                return Sort.by(sortDirection, "createdAt");
            case "price":
                return Sort.by(sortDirection, "price");
            default:
                return Sort.by(Sort.Direction.DESC, "likeCount"); // 默认按点赞数降序
        }
    }

    /**
     * 获取搜索建议
     */
    public List<SearchSuggestionResponse> getSearchSuggestions(String query) {
        if (!StringUtils.hasText(query) || query.length() < 2) {
            return Collections.emptyList();
        }

        try {
            Pageable pageable = PageRequest.of(0, 10);
            List<String> suggestions = searchLogRepository.findSimilarKeywords(query, pageable);
            
            return suggestions.stream()
                .map(keyword -> {
                    Long count = searchLogRepository.countBySearchKeyword(keyword);
                    return new SearchSuggestionResponse(keyword, count, "KEYWORD");
                })
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                
        } catch (Exception e) {
            log.warn("获取搜索建议失败: {}", e.getMessage());
            return getDefaultSearchSuggestions(query);
        }
    }
    
    /**
     * 获取默认搜索建议
     */
    private List<SearchSuggestionResponse> getDefaultSearchSuggestions(String query) {

        // 模拟搜索建议
        List<SearchSuggestionResponse> suggestions = new ArrayList<>();
        String[] mockSuggestions = {
            query + "教程", query + "入门", query + "实践",
            query + "进阶", query + "源码", query + "架构"
        };

        for (int i = 0; i < mockSuggestions.length && i < 6; i++) {
            suggestions.add(new SearchSuggestionResponse(
                mockSuggestions[i], 
                (long) (Math.random() * 100), 
                "KEYWORD"
            ));
        }

        return suggestions;
    }

    /**
     * 获取热门搜索关键词
     */
    public List<String> getHotKeywords(int limit) {
        try {
            // 获取过去30天的热门关键词
            LocalDateTime startTime = LocalDateTime.now().minusDays(30);
            Pageable pageable = PageRequest.of(0, limit);
            
            List<Object[]> results = searchLogRepository.findPopularKeywords(startTime, pageable);
            
            return results.stream()
                .map(result -> (String) result[0])
                .filter(StringUtils::hasText)
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
                
        } catch (Exception e) {
            log.warn("获取热门关键词失败: {}", e.getMessage());
            // 返回默认热门关键词
            return getDefaultHotKeywords(limit);
        }
    }
    
    /**
     * 获取默认热门关键词
     */
    private List<String> getDefaultHotKeywords(int limit) {
        // 模拟热门关键词
        List<String> hotKeywords = Arrays.asList(
            "React", "Vue", "Java", "Python", "Spring Boot",
            "微服务", "前端", "后端", "数据库", "算法",
            "机器学习", "区块链", "云计算", "DevOps", "Docker"
        );
        
        return hotKeywords.stream()
            .limit(limit)
            .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);
    }

    /**
     * 获取用户搜索历史
     */
    public List<String> getUserSearchHistory(String userId, int limit) {
        if (!StringUtils.hasText(userId)) {
            return Collections.emptyList();
        }
        
        try {
            Pageable pageable = PageRequest.of(0, limit);
            return searchLogRepository.findUserSearchHistory(userId, pageable);
        } catch (Exception e) {
            log.warn("获取用户搜索历史失败: {}", e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 记录搜索日志
     */
    private void recordSearchLog(SearchRequest request) {
        if (!StringUtils.hasText(request.getKeyword())) {
            return;
        }

        try {
            SearchLog searchLog = new SearchLog();
            searchLog.setUserId(request.getUserId());
            searchLog.setSearchKeyword(request.getKeyword());
            searchLog.setSearchType(SearchLog.SearchType.valueOf(
                request.getSearchType() != null ? request.getSearchType() : "COMPREHENSIVE"));
            searchLog.setResultCount(0); // 将在搜索完成后更新
            searchLog.setCreatedAt(LocalDateTime.now());
            
            searchLogRepository.save(searchLog);
            
            log.info("记录搜索日志: 用户={}, 关键词={}, 搜索类型={}", 
                request.getUserId(), request.getKeyword(), request.getSearchType());
        } catch (Exception e) {
            log.warn("记录搜索日志失败: {}", e.getMessage());
        }
    }

    /**
     * 更新搜索结果数量
     */
    private void updateSearchResultCount(SearchRequest request, int resultCount) {
        if (!StringUtils.hasText(request.getKeyword())) {
            return;
        }

        try {
            log.info("更新搜索结果数量: 关键词={}, 结果数={}", 
                request.getKeyword(), resultCount);
        } catch (Exception e) {
            log.warn("更新搜索结果数量失败: {}", e.getMessage());
        }
    }

    /**
     * 清理旧的搜索日志（定期任务）
     */
    public void cleanupOldSearchLogs() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(90);
            searchLogRepository.deleteByCreatedAtBefore(cutoffTime);
            log.info("清理90天前的搜索日志完成");
        } catch (Exception e) {
            log.error("清理旧搜索日志失败: {}", e.getMessage());
        }
    }
}