package com.yunke.backend.ai.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * AI配额管理实体
 * 对应Node.js版本的CopilotQuota
 */
@Entity
@Table(name = "copilot_quotas", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"user_id", "feature"}),
        @UniqueConstraint(columnNames = {"workspace_id", "feature"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CopilotQuota {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", length = 36)
    private String userId;

    @Column(name = "workspace_id", length = 36)
    private String workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature", nullable = false, length = 50)
    private CopilotFeature feature;

    @Column(name = "limit_per_day")
    private Integer limitPerDay;

    @Column(name = "limit_per_month")
    private Integer limitPerMonth;

    @Column(name = "used_today")
    private Integer usedToday;

    @Column(name = "used_this_month")
    private Integer usedThisMonth;

    @Column(name = "token_limit_per_day")
    private Integer tokenLimitPerDay;

    @Column(name = "token_limit_per_month")
    private Integer tokenLimitPerMonth;

    @Column(name = "tokens_used_today")
    private Integer tokensUsedToday;

    @Column(name = "tokens_used_this_month")
    private Integer tokensUsedThisMonth;

    @Column(name = "last_reset_date")
    private LocalDateTime lastResetDate;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public enum CopilotFeature {
        CHAT("chat"),
        IMAGE_GENERATION("image_generation"),
        TEXT_GENERATION("text_generation"),
        CODE_COMPLETION("code_completion"),
        TRANSLATION("translation"),
        SUMMARIZATION("summarization");

        private final String value;

        CopilotFeature(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }

    public boolean canUseFeature() {
        return hasRequestQuota() && hasTokenQuota();
    }

    public boolean hasRequestQuota() {
        if (limitPerDay != null && usedToday != null && usedToday >= limitPerDay) {
            return false;
        }
        if (limitPerMonth != null && usedThisMonth != null && usedThisMonth >= limitPerMonth) {
            return false;
        }
        return true;
    }

    public boolean hasTokenQuota() {
        if (tokenLimitPerDay != null && tokensUsedToday != null && tokensUsedToday >= tokenLimitPerDay) {
            return false;
        }
        if (tokenLimitPerMonth != null && tokensUsedThisMonth != null && tokensUsedThisMonth >= tokenLimitPerMonth) {
            return false;
        }
        return true;
    }

    public void incrementUsage(int tokens) {
        this.usedToday = (this.usedToday != null ? this.usedToday : 0) + 1;
        this.usedThisMonth = (this.usedThisMonth != null ? this.usedThisMonth : 0) + 1;
        this.tokensUsedToday = (this.tokensUsedToday != null ? this.tokensUsedToday : 0) + tokens;
        this.tokensUsedThisMonth = (this.tokensUsedThisMonth != null ? this.tokensUsedThisMonth : 0) + tokens;
    }

    public void resetDaily() {
        this.usedToday = 0;
        this.tokensUsedToday = 0;
        this.lastResetDate = LocalDateTime.now();
    }

    public void resetMonthly() {
        this.usedThisMonth = 0;
        this.tokensUsedThisMonth = 0;
        this.lastResetDate = LocalDateTime.now();
    }

    public double getRequestUsagePercent() {
        if (limitPerDay == null || limitPerDay == 0) return 0.0;
        return (double) (usedToday != null ? usedToday : 0) / limitPerDay * 100;
    }

    public double getTokenUsagePercent() {
        if (tokenLimitPerDay == null || tokenLimitPerDay == 0) return 0.0;
        return (double) (tokensUsedToday != null ? tokensUsedToday : 0) / tokenLimitPerDay * 100;
    }

    public int getRemainingRequests() {
        if (limitPerDay == null) return Integer.MAX_VALUE;
        return Math.max(0, limitPerDay - (usedToday != null ? usedToday : 0));
    }

    public int getRemainingTokens() {
        if (tokenLimitPerDay == null) return Integer.MAX_VALUE;
        return Math.max(0, tokenLimitPerDay - (tokensUsedToday != null ? tokensUsedToday : 0));
    }
}