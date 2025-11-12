package com.yunke.backend.notification.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * 邮件模板分页查询请求DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TemplateQueryDto {
    
    /**
     * 页码（从0开始）
     */
    private Integer page = 0;
    
    /**
     * 每页大小
     */
    private Integer size = 20;
    
    /**
     * 搜索关键词
     */
    private String keyword;
    
    /**
     * 模板分类
     */
    private String category;
    
    /**
     * 语言代码
     */
    private String language;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 排序字段
     */
    private String sortBy = "updatedAt";
    
    /**
     * 排序方向
     */
    private String sortDirection = "desc";
    
    /**
     * 排序字段列表
     */
    private List<String> sort;
}