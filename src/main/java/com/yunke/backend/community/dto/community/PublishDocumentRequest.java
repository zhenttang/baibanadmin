package com.yunke.backend.community.dto.community;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.List;

/**
 * 发布文档到社区请求DTO
 */
@Data
public class PublishDocumentRequest {
    
    @NotBlank(message = "文档标题不能为空")
    private String title;
    
    private String description;
    
    @NotBlank(message = "文档内容URL不能为空")
    private String contentUrl;
    
    private Integer categoryId;
    
    private List<String> tagNames;
    
    @NotNull(message = "必须指定是否为付费文档")
    private Boolean isPaid;
    
    @DecimalMin(value = "0.0", message = "价格不能为负数")
    private BigDecimal price;
    
    @NotNull(message = "必须指定是否公开")
    private Boolean isPublic;
    
    @NotNull(message = "必须指定是否需要关注")
    private Boolean requireFollow;
}