package com.yunke.backend.payment.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentStatsDTO {
    private BigDecimal totalRevenue;
    private BigDecimal monthlyRevenue;
    private BigDecimal dailyRevenue;
    private Integer totalTransactions;
    private Integer monthlyTransactions;
    private Integer dailyTransactions;
    private Integer activeSubscriptions;
    private BigDecimal averageTransactionValue;
    private Map<String, BigDecimal> revenueByProvider;
    private List<DailyStats> dailyStats;
    private LocalDateTime lastUpdated;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStats {
        private LocalDateTime date;
        private BigDecimal revenue;
        private Integer transactions;
        private Integer newSubscriptions;
        private Integer cancelledSubscriptions;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderStats {
        private String provider;
        private BigDecimal revenue;
        private Integer transactions;
        private Double successRate;
        private BigDecimal averageValue;
    }
}
