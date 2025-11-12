package com.yunke.backend.community.dto.community;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 标签信息响应DTO
 */
@Data
public class TagInfo {
    
    private Integer id;
    
    private String name;
    
    private String color;
    
    private Integer usageCount;
    
    private LocalDateTime createdAt;
}