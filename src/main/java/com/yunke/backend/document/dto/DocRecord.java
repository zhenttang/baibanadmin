package com.yunke.backend.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档记录
 * 对应Node.js版本的DocRecord接口
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocRecord {
    
    /**
     * 工作空间ID
     */
    private String spaceId;
    
    /**
     * 文档ID
     */
    private String docId;
    
    /**
     * YJS序列化的二进制数据
     */
    private byte[] blob;
    
    /**
     * 时间戳（毫秒）
     */
    private Long timestamp;
    
    /**
     * 编辑者ID
     */
    private String editorId;
}