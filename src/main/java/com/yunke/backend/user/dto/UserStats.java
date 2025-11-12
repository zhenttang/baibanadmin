package com.yunke.backend.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 用户统计信息DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStats {
    /**
     * 文档数量
     */
    private int documentCount;
    
    /**
     * 工作空间数量
     */
    private int workspaceCount;
    
    /**
     * 存储使用量（字节）
     */
    private long storageUsed;
    
    /**
     * 协作人数
     */
    private int collaboratorCount;
}