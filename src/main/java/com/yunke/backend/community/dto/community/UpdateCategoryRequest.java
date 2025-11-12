package com.yunke.backend.community.dto.community;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 更新分类请求DTO
 */
@Data
public class UpdateCategoryRequest {
    
    @NotBlank(message = "分类名称不能为空")
    @Size(max = 50, message = "分类名称长度不能超过50个字符")
    private String name;
    
    @Size(max = 200, message = "分类描述长度不能超过200个字符")
    private String description;
    
    @Size(max = 100, message = "图标路径长度不能超过100个字符")
    private String icon;
    
    private Integer sortOrder;
    
    private Boolean isActive;
}