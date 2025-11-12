package com.yunke.backend.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档差异DTO - 完全参考AFFiNE的DocDiff接口
 * 
 * 对应AFFiNE中的 DocDiff interface:
 * - missing: Uint8Array
 * - state: Uint8Array  
 * - timestamp: number
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocDiffDto {
    
    /**
     * 缺失的更新数据
     * 客户端需要应用的YJS更新
     */
    private byte[] missing;
    
    /**
     * 当前状态向量
     * 用于后续增量同步
     */
    private byte[] state;
    
    /**
     * 文档时间戳
     * 表示当前文档版本的时间
     */
    private Long timestamp;
}