package com.yunke.backend.system.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import com.yunke.backend.user.domain.entity.UserSession;

@Entity
@Table(name = "multiple_users_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    @Id
    private String id;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime deprecatedExpiresAt;

    @OneToMany(mappedBy = "session", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserSession> userSessions = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    // 手动添加缺失的方法 - 临时解决Lombok编译问题
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // 手动添加builder方法
    public static SessionBuilder builder() {
        return new SessionBuilder();
    }
    
    public static class SessionBuilder {
        private String id;
        private LocalDateTime createdAt;
        
        public SessionBuilder id(String id) { this.id = id; return this; }
        public SessionBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        
        public Session build() {
            Session session = new Session();
            session.id = this.id;
            session.createdAt = this.createdAt;
            return session;
        }
    }
} 