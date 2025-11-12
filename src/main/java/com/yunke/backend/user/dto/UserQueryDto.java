package com.yunke.backend.user.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 用户查询请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserQueryDto {
    
    /**
     * 搜索关键词（用户名、邮箱）
     */
    private String search;
    
    /**
     * 筛选条件：是否启用
     */
    private Boolean enabled;
    
    /**
     * 筛选条件：是否已注册
     */
    private Boolean registered;
    
    /**
     * 筛选条件：邮箱是否已验证
     */
    private Boolean emailVerified;
    
    /**
     * 筛选条件：是否为管理员
     */
    private Boolean isAdmin;
    
    /**
     * 排序字段
     */
    private String sortBy;
    
    /**
     * 排序方向（asc, desc）
     */
    private String sortDirection;
    
    /**
     * 页码（从0开始）
     */
    private Integer page;
    
    /**
     * 每页大小
     */
    private Integer size;
    
    /**
     * 特性筛选
     */
    private List<String> features;
    
    /**
     * 创建时间范围 - 开始
     */
    private String createdAfter;
    
    /**
     * 创建时间范围 - 结束
     */
    private String createdBefore;
    
    /**
     * 最后登录时间范围 - 开始
     */
    private String lastLoginAfter;
    
    /**
     * 最后登录时间范围 - 结束
     */
    private String lastLoginBefore;
}