package com.yunke.backend.community.dto.community;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 分类信息响应DTO
 */
@Data
public class CategoryInfo {
    
    private Integer id;
    
    private String name;
    
    private String description;
    
    private String icon;
    
    private Integer sortOrder;
    
    private Boolean isActive;
    
    private Integer documentCount;
    
    private LocalDateTime createdAt;
}