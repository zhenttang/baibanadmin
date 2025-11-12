package com.yunke.backend.common.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 分页输入参数
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaginationInput {
    @Builder.Default
    private Integer page = 0;
    
    @Builder.Default
    private Integer size = 10;
    
    private String sortBy;
    private Boolean ascending;
    
    /**
     * 获取偏移量
     */
    public Integer getOffset() {
        return page * size;
    }
    
    /**
     * 获取每页数量
     */
    public Integer getFirst() {
        return size;
    }
} 