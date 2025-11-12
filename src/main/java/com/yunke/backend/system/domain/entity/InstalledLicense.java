package com.yunke.backend.system.domain.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "installed_licenses", uniqueConstraints = {
    @UniqueConstraint(name = "uk_installed_license_workspace", columnNames = {"workspace_id"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InstalledLicense {

    @Id
    @Column(name = "key", nullable = false)
    private String key;

    @Column(name = "workspace_id", nullable = false, unique = true)
    private String workspaceId;

    @Column(name = "quantity", nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "recurring", nullable = false)
    private String recurring;

    @Column(name = "variant")
    private String variant;

    @CreationTimestamp
    @Column(name = "installed_at", nullable = false, updatable = false)
    private LocalDateTime installedAt;

    @Column(name = "validate_key", nullable = false)
    private String validateKey;

    @Column(name = "validated_at", nullable = false)
    private LocalDateTime validatedAt;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;

    @Lob
    @Column(name = "license", columnDefinition = "BYTEA")
    private byte[] license;
} 