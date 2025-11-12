package com.yunke.backend.infrastructure.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import java.net.URI;

/**
 * 存储配置
 */
@Configuration
@EnableConfigurationProperties(AffineConfig.class)
@RequiredArgsConstructor
@Slf4j
public class StorageConfig {

    private final AffineConfig affineConfig;

    /**
     * S3 客户端配置 - 只在非本地存储时创建
     */
    @Bean
    @ConditionalOnExpression("!'${affine.storage.provider:LOCAL}'.equals('LOCAL')")
    public S3Client s3Client() {
        AffineConfig.StorageConfig storageConfig = affineConfig.getStorage();
        
        try {
            var builder = S3Client.builder();
            
            // 设置认证信息
            if (storageConfig.getAccessKeyId() != null && storageConfig.getSecretAccessKey() != null) {
                AwsBasicCredentials credentials = AwsBasicCredentials.create(
                        storageConfig.getAccessKeyId(),
                        storageConfig.getSecretAccessKey()
                );
                builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
            }
            
            // 设置区域
            if (storageConfig.getRegion() != null) {
                builder.region(Region.of(storageConfig.getRegion()));
            }
            
            // 设置端点（用于 R2, COS 等）
            if (storageConfig.getEndpoint() != null) {
                builder.endpointOverride(URI.create(storageConfig.getEndpoint()));
            }
            
            // 对于 R2 和 COS，需要强制使用路径样式
            if (storageConfig.getProvider() == AffineConfig.StorageProvider.R2 ||
                storageConfig.getProvider() == AffineConfig.StorageProvider.COS) {
                builder.forcePathStyle(true);
            }
            
            S3Client client = builder.build();
            
            log.info("S3 client initialized for provider: {}", storageConfig.getProvider());
            return client;
        } catch (Exception e) {
            log.error("Failed to initialize S3 client", e);
            return null;
        }
    }

    /**
     * S3 预签名器配置 - 只在非本地存储时创建
     */
    @Bean
    @ConditionalOnExpression("!'${affine.storage.provider:LOCAL}'.equals('LOCAL')")
    public S3Presigner s3Presigner() {
        AffineConfig.StorageConfig storageConfig = affineConfig.getStorage();
        
        try {
            S3Presigner.Builder builder = S3Presigner.builder();
            
            // 设置认证信息
            if (storageConfig.getAccessKeyId() != null && storageConfig.getSecretAccessKey() != null) {
                AwsBasicCredentials credentials = AwsBasicCredentials.create(
                        storageConfig.getAccessKeyId(),
                        storageConfig.getSecretAccessKey()
                );
                builder.credentialsProvider(StaticCredentialsProvider.create(credentials));
            }
            
            // 设置区域
            if (storageConfig.getRegion() != null) {
                builder.region(Region.of(storageConfig.getRegion()));
            }
            
            // 设置端点（用于 R2, COS 等）
            if (storageConfig.getEndpoint() != null) {
                builder.endpointOverride(URI.create(storageConfig.getEndpoint()));
            }
            
            S3Presigner presigner = builder.build();
            
            log.info("S3 presigner initialized for provider: {}", storageConfig.getProvider());
            return presigner;
        } catch (Exception e) {
            log.error("Failed to initialize S3 presigner", e);
            return null;
        }
    }
}