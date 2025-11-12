package com.yunke.backend.user.domain.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users", indexes = {@Index(name = "idx_user_email", columnList = "email")})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "email_verified")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "avatar_url")
    private String avatarUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonIgnore
    @Column
    private String password;

    @Column(nullable = false)
    @Builder.Default
    private boolean registered = true;

    @Column(nullable = false)
    @Builder.Default
    private boolean disabled = false;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "registered_at")
    private Instant registeredAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    // 暂时移除所有关系映射，避免缺失表的问题
    // 先让基本的用户认证功能运行起来，之后再逐步添加关联
    
    /*
    // 关系映射 - 暂时注释掉
    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserFeature> features = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserStripeCustomer userStripeCustomer;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkspaceUserRole> workspaces = new ArrayList<>();

    @OneToMany(mappedBy = "inviter", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkspaceUserRole> WorkspaceInvitations = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkspaceDocUserRole> docPermissions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ConnectedAccount> connectedAccounts = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserSession> sessions = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AiSession> aiSessions = new ArrayList<>();

    @OneToMany(mappedBy = "lastUpdatedByUser", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DeprecatedAppRuntimeSettings> deprecatedAppRuntimeSettings = new ArrayList<>();

    @OneToMany(mappedBy = "lastUpdatedByUser", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AppConfig> appConfigs = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserSnapshot> userSnapshots = new ArrayList<>();

    @OneToMany(mappedBy = "createdByUser", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Snapshot> createdSnapshot = new ArrayList<>();

    @OneToMany(mappedBy = "updatedByUser", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Snapshot> updatedSnapshot = new ArrayList<>();

    @OneToMany(mappedBy = "createdByUser", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Update> createdUpdate = new ArrayList<>();

    @OneToMany(mappedBy = "createdByUser", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SnapshotHistory> createdHistory = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AiJobs> createdAiJobs = new ArrayList<>();

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Notification> notifications = new ArrayList<>();

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private UserSettings settings;
    */

    @PrePersist
    protected void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID().toString();
        }
    }

    // 手动添加缺失的方法 - 临时解决Lombok编译问题
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // 手动添加builder方法
    public static UserBuilder builder() {
        return new UserBuilder();
    }
    
    public static class UserBuilder {
        private String id;
        private String name;
        private String email;
        private String password;
        private LocalDateTime createdAt;
        private String avatarUrl;
        private boolean registered = true;
        private boolean disabled = false;
        private boolean enabled = true;
        
        public UserBuilder id(String id) { this.id = id; return this; }
        public UserBuilder name(String name) { this.name = name; return this; }
        public UserBuilder email(String email) { this.email = email; return this; }
        public UserBuilder password(String password) { this.password = password; return this; }
        public UserBuilder createdAt(LocalDateTime createdAt) { this.createdAt = createdAt; return this; }
        public UserBuilder avatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; return this; }
        public UserBuilder registered(boolean registered) { this.registered = registered; return this; }
        public UserBuilder disabled(boolean disabled) { this.disabled = disabled; return this; }
        public UserBuilder enabled(boolean enabled) { this.enabled = enabled; return this; }
        
        public User build() {
            User user = new User();
            user.id = this.id;
            user.name = this.name;
            user.email = this.email;
            user.password = this.password;
            user.createdAt = this.createdAt;
            user.avatarUrl = this.avatarUrl;
            user.registered = this.registered;
            user.disabled = this.disabled;
            user.enabled = this.enabled;
            return user;
        }
    }
} 