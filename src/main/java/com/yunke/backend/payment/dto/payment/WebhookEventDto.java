package com.yunke.backend.payment.dto.payment;

import com.yunke.backend.payment.PaymentProvider;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * 支付webhook事件DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebhookEventDto {
    private String id;
    private String type;
    private PaymentProvider.PaymentProviderType provider;
    private Map<String, Object> data;
    private String signature;
    private String rawPayload;
    private boolean processed;
    
    public static class WebhookEventDtoBuilder {
        public WebhookEventDtoBuilder provider(PaymentProvider.PaymentProviderType provider) {
            this.provider = provider;
            return this;
        }
        
        public WebhookEventDtoBuilder processed(boolean processed) {
            this.processed = processed;
            return this;
        }
    }
} 