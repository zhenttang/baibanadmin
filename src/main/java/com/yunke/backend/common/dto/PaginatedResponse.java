package com.yunke.backend.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页响应对象
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginatedResponse<T> {
    private List<T> content;
    private long totalElements;
    private int totalPages;
    private int currentPage;  // 重命名为currentPage以匹配控制器
    private int size;         // 重命名为size以匹配控制器
    private boolean hasNext;  // 添加hasNext
    private boolean hasPrevious; // 添加hasPrevious
    private boolean last;
    private boolean first;
    
    // 为了兼容前端的响应格式，添加这些字段
    private List<T> data;
    private long total;
    private boolean hasMore;
    
    public static <T> PaginatedResponse<T> of(List<T> content, long totalElements, int pageNumber, int pageSize) {
        int totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / (double) pageSize) : 1;
        boolean isLast = pageNumber >= totalPages - 1;
        boolean isFirst = pageNumber == 0;
        boolean hasNext = pageNumber < totalPages - 1;
        boolean hasPrevious = pageNumber > 0;
        
        return PaginatedResponse.<T>builder()
                .content(content)
                .totalElements(totalElements)
                .totalPages(totalPages)
                .currentPage(pageNumber)
                .size(pageSize)
                .hasNext(hasNext)
                .hasPrevious(hasPrevious)
                .last(isLast)
                .first(isFirst)
                .data(content) // 兼容字段
                .total(totalElements) // 兼容字段
                .hasMore(hasNext) // 兼容字段
                .build();
    }
} 