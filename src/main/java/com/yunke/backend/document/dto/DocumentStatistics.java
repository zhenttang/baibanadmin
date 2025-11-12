package com.yunke.backend.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentStatistics {
    private Integer viewCount;
    private Integer likeCount;
    private Integer collectCount;
    private Integer commentCount;
    private Integer shareCount;
    private Integer purchaseCount;
    private BigDecimal qualityScore;
    private BigDecimal avgRating;
    private Integer ratingCount;
    private Long uniqueViewers;
    private Double avgViewDuration;
}
