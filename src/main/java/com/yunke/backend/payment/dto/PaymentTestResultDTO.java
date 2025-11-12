package com.yunke.backend.payment.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentTestResultDTO {
    private String testId;
    private String provider;
    private Boolean success;
    private String status;
    private LocalDateTime timestamp;
    private Integer responseTime;
    private String errorMessage;
    private List<TestStep> steps;
    private TestDetails details;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestStep {
        private String name;
        private String description;
        private Boolean success;
        private String result;
        private String errorMessage;
        private Integer duration;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestDetails {
        private String transactionId;
        private String amount;
        private String currency;
        private String paymentMethod;
        private String webhookReceived;
        private String refundStatus;
    }
}
