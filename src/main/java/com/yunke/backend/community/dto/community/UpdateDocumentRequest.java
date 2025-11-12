package com.yunke.backend.community.dto.community;

import lombok.Data;
import jakarta.validation.constraints.DecimalMin;
import java.math.BigDecimal;
import java.util.List;

/**
 * 更新社区文档请求DTO
 */
@Data
public class UpdateDocumentRequest {
    
    private String title;
    private String description;
    private String contentUrl;
    private Integer categoryId;
    private List<String> tagNames;
    private Boolean isPaid;
    
    @DecimalMin(value = "0.0", message = "价格不能为负数")
    private BigDecimal price;
    
    private Boolean isPublic;
    private Boolean requireFollow;
}