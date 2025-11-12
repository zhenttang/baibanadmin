package com.yunke.backend.payment.dto.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 产品DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private String id;
    private String name;
    private String description;
    private String type;
    private Boolean active;
    private String imageUrl;
    private List<String> features;
    private LocalDateTime createdAt;
    private Map<String, Object> metadata;
    private List<PriceDto> prices;
} 