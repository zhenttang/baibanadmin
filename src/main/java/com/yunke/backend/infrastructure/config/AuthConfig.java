package com.yunke.backend.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.EqualsAndHashCode;

/**
 * 认证系统配置
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "affine.auth")
public class AuthConfig {

    /**
     * JWT配置
     */
    @NotNull
    private Jwt jwt = new Jwt();

    /**
     * 是否允许注册
     */
    private boolean allowSignup = true;

    /**
     * 是否要求邮箱域名验证
     */
    private boolean requireEmailDomainVerification = false;

    /**
     * 是否要求邮箱验证
     */
    private boolean requireEmailVerification = true;

    /**
     * 密码要求配置
     */
    @NotNull
    private PasswordRequirements passwordRequirements = new PasswordRequirements();

    /**
     * OAuth配置
     */
    @NotNull
    private OAuth oauth = new OAuth();

    @Data
    public static class Jwt {
        /**
         * JWT密钥 - 优先从环境变量获取
         */
        private String secret = System.getenv("JWT_SECRET") != null ? 
            System.getenv("JWT_SECRET") : "affine_jwt_secret_key_please_change_in_production";

        /**
         * JWT过期时间(毫秒) - 默认7天
         */
        @Min(3600000) // 最少1小时
        private long expiration = 604800000L; // 7天

        /**
         * JWT刷新令牌过期时间(毫秒) - 默认30天
         */
        @Min(86400000) // 最少1天
        private long refreshExpiration = 2592000000L; // 30天

        /**
         * JWT发行者
         */
        private String issuer = "affine";
        
        /**
         * 是否启用JWT黑名单
         */
        private boolean enableBlacklist = true;
        
        /**
         * Token自动刷新阈值（剩余时间小于此值时自动刷新，毫秒）
         */
        private long autoRefreshThreshold = 3600000L; // 1小时
        
        /**
         * 最大同时登录会话数（0表示无限制）
         */
        private int maxSessions = 5;
        
        /**
         * 是否记录JWT操作日志
         */
        private boolean enableAuditLog = true;
    }

    @Data
    public static class PasswordRequirements {
        /**
         * 密码最小长度
         */
        @Min(1)
        private int min = 8;

        /**
         * 密码最大长度
         */
        @Max(128)
        private int max = 32;

        /**
         * 验证密码长度配置
         */
        public void validate() {
            if (min >= max) {
                throw new IllegalArgumentException("Password min length must be less than max length");
            }
        }
    }

    @Data
    public static class OAuth {
        /**
         * OAuth状态过期时间(秒) - 默认3小时
         */
        @Min(600) // 最少10分钟
        private long stateExpiry = 10800L; // 3小时

        /**
         * Google OAuth配置
         */
        private OAuthProvider google = new OAuthProvider();

        /**
         * GitHub OAuth配置
         */
        private OAuthProvider github = new OAuthProvider();

        /**
         * Apple OAuth配置
         */
        private OAuthProvider apple = new OAuthProvider();

        /**
         * OIDC OAuth配置
         */
        private OidcProvider oidc = new OidcProvider();
    }

    @Data
    public static class OAuthProvider {
        /**
         * 是否启用
         */
        private boolean enabled = false;

        /**
         * 客户端ID
         */
        private String clientId;

        /**
         * 客户端密钥
         */
        private String clientSecret;

        /**
         * 授权URL
         */
        private String authUrl;

        /**
         * Token URL
         */
        private String tokenUrl;

        /**
         * 用户信息URL
         */
        private String userInfoUrl;

        /**
         * 授权范围
         */
        private String scope;

        /**
         * 额外参数
         */
        private java.util.Map<String, String> args = new java.util.HashMap<>();
    }

    @Data
    @EqualsAndHashCode(callSuper=false)
    public static class OidcProvider extends OAuthProvider {
        /**
         * OIDC发行者URL
         */
        private String issuer;

        /**
         * 用户ID声明字段
         */
        private String claimId = "sub";

        /**
         * 邮箱声明字段
         */
        private String claimEmail = "email";

        /**
         * 姓名声明字段
         */
        private String claimName = "name";
    }

    /**
     * 验证配置
     */
    public void validate() {
        passwordRequirements.validate();
    }
}