package com.yunke.backend.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档权限信息
 * 用于HTTP响应头中的权限控制
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocPermissionInfo {
    
    /**
     * 发布模式
     * - "page": 页面模式（默认）
     * - "edgeless": 白板模式
     */
    @Builder.Default
    private String publishMode = "page";
    
    /**
     * 权限模式
     * - "private": 私有（默认）
     * - "read-only": 只读
     * - "append-only": 仅追加
     */
    @Builder.Default
    private String permissionMode = "private";
    
    /**
     * 是否为公开文档
     */
    @Builder.Default
    private boolean isPublic = false;
}