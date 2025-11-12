package com.yunke.backend.community.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 收藏文档请求
 */
@Data
public class CollectRequest {
    
    /**
     * 收藏夹名称
     */
    @NotBlank(message = "收藏夹名称不能为空")
    @Size(max = 100, message = "收藏夹名称不能超过100个字符")
    private String collectionName = "默认收藏夹";
}