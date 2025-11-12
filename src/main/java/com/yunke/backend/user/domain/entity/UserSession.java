package com.yunke.backend.user.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;
import com.yunke.backend.system.domain.entity.Session;

@Entity
@Table(
    name = "user_sessions", 
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_session_user", columnNames = {"session_id", "user_id"})
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    @Id
    private String id;

    @Column(name = "session_id", nullable = false)
    private String sessionId;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Session session;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    private User user;

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    // 手动添加缺失的方法 - 临时解决Lombok编译问题
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public void setExpiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; }
    
    // 手动添加builder方法
    public static UserSessionBuilder builder() {
        return new UserSessionBuilder();
    }
    
    public static class UserSessionBuilder {
        private String id;
        private String sessionId;
        private String userId;
        private LocalDateTime expiresAt;
        
        public UserSessionBuilder id(String id) { this.id = id; return this; }
        public UserSessionBuilder sessionId(String sessionId) { this.sessionId = sessionId; return this; }
        public UserSessionBuilder userId(String userId) { this.userId = userId; return this; }
        public UserSessionBuilder expiresAt(LocalDateTime expiresAt) { this.expiresAt = expiresAt; return this; }
        
        public UserSession build() {
            UserSession userSession = new UserSession();
            userSession.id = this.id;
            userSession.sessionId = this.sessionId;
            userSession.userId = this.userId;
            userSession.expiresAt = this.expiresAt;
            return userSession;
        }
    }
} 