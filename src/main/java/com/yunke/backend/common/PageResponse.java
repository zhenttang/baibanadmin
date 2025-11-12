package com.yunke.backend.common;

import lombok.Data;

import java.util.List;

/**
 * 分页响应格式
 */
@Data
public class PageResponse<T> {
    
    /**
     * 数据列表
     */
    private List<T> items;
    
    /**
     * 当前页码
     */
    private int page;
    
    /**
     * 每页大小
     */
    private int size;
    
    /**
     * 总记录数
     */
    private long total;
    
    /**
     * 总页数
     */
    private int totalPages;
}