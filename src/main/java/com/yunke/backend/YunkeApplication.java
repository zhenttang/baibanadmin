package com.yunke.backend;

import com.yunke.backend.infrastructure.config.AffineConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Yunke Java后端应用程序入口
 * 
 * 基于 Spring Boot 3.x 构建的 Yunke 协作平台后端服务
 * 
 * 主要功能:
 * - 用户认证和授权
 * - 工作空间管理
 * - 文档协作
 * - AI 集成
 * - 通知系统
 * - 支付管理
 * 
 * @author Yunke Team
 * @version 0.21.0
 */
@SpringBootApplication
@EnableConfigurationProperties(AffineConfig.class)
@EnableCaching
@EnableScheduling
@EnableAsync
@Slf4j
public class YunkeApplication {

    public static void main(String[] args) {
        // 设置系统属性
        System.setProperty("spring.application.name", "yunke-java-backend");
        System.setProperty("file.encoding", "UTF-8");
        System.setProperty("user.timezone", "UTC");
        
        // 启动 Spring Boot 应用
        ConfigurableApplicationContext context = SpringApplication.run(YunkeApplication.class, args);
        
        // 打印启动信息
        printStartupInfo(context);
    }

    /**
     * 打印应用启动信息
     */
    private static void printStartupInfo(ConfigurableApplicationContext context) {
        AffineConfig config = context.getBean(AffineConfig.class);
        
        log.info("=================================================================");
        log.info("                    Yunke Java Backend Started                 ");
        log.info("=================================================================");
        log.info("🚀 Application: {}", context.getEnvironment().getProperty("spring.application.name"));
        log.info("🌐 Server URL: {}", config.getServer().getExternalUrl() != null ? 
                config.getServer().getExternalUrl() : "N/A");
        log.info("📊 Database: {}", config.getDatabase().getUrl() != null ? 
                config.getDatabase().getUrl() : "N/A");
        log.info("🔄 Redis: {}:{}", 
                config.getRedis().getHost(), 
                config.getRedis().getPort());
        log.info("💾 Storage: {} ({})", 
                config.getStorage().getProvider(), 
                (config.getStorage().getProvider() == AffineConfig.StorageProvider.LOCAL) 
                        ? config.getStorage().getLocalPath() : "Cloud Storage");
        log.info("🤖 AI Enabled: {}", config.getCopilot().isEnabled());
        log.info("💳 Payment Enabled: {}", config.getPayment().isEnabled());
        log.info("📧 Email Enabled: {}", 
                config.getNotification().getEmail().isEnabled());
        log.info("🔧 Flavor: {}", config.getServer().getFlavor());
        log.info("🏗️  Deployment: {}", config.getServer().getDeployment());
        log.info("📈 Actuator: http://{}:{}/actuator", 
                config.getServer().getHost(), 
                config.getServer().getPort());
        
        if (context.getEnvironment().acceptsProfiles(org.springframework.core.env.Profiles.of("dev"))) {
            log.info("🔍 GraphiQL: http://{}:{}/graphiql", 
                    config.getServer().getHost(), 
                    config.getServer().getPort());
        }
        
        log.info("=================================================================");
        log.info("✅ Yunke Java Backend is ready to serve requests!");
        log.info("=================================================================");
    }
} 