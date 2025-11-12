package com.yunke.backend.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.NestedConfigurationProperty;

/**
 * AFFiNE 应用程序配置
 * 对应 Node.js 版本的 AppConfigType
 */
@Data
@ConfigurationProperties(prefix = "affine")
public class AffineConfig {

    /**
     * 服务器配置
     */
    @NestedConfigurationProperty
    private ServerConfig server = new ServerConfig();

    /**
     * 数据库配置
     */
    @NestedConfigurationProperty
    private DatabaseConfig database = new DatabaseConfig();

    /**
     * Redis 配置
     */
    @NestedConfigurationProperty
    private RedisConfig redis = new RedisConfig();

    /**
     * 存储配置
     */
    @NestedConfigurationProperty
    private StorageConfig storage = new StorageConfig();

    /**
     * AI 配置
     */
    @NestedConfigurationProperty
    private CopilotConfig copilot = new CopilotConfig();

    /**
     * 文档服务配置
     */
    @NestedConfigurationProperty
    private DocServiceConfig docService = new DocServiceConfig();

    /**
     * 支付配置
     */
    @NestedConfigurationProperty
    private PaymentConfig payment = new PaymentConfig();

    /**
     * 认证配置
     */
    @NestedConfigurationProperty
    private AuthConfig auth = new AuthConfig();

    /**
     * 通知配置
     */
    @NestedConfigurationProperty
    private NotificationConfig notification = new NotificationConfig();

    /**
     * 获取服务器配置
     */
    public ServerConfig getServer() {
        return server;
    }

    /**
     * 获取数据库配置
     */
    public DatabaseConfig getDatabase() {
        return database;
    }

    /**
     * 获取Redis配置
     */
    public RedisConfig getRedis() {
        return redis;
    }

    /**
     * 获取存储配置
     */
    public StorageConfig getStorage() {
        return storage;
    }
    
    /**
     * 获取AI配置
     */
    public CopilotConfig getCopilot() {
        return copilot;
    }
    
    /**
     * 获取支付配置
     */
    public PaymentConfig getPayment() {
        return payment;
    }
    
    /**
     * 获取通知配置
     */
    public NotificationConfig getNotification() {
        return notification;
    }
    
    /**
     * 获取认证配置
     */
    public AuthConfig getAuth() {
        return auth;
    }

    /**
     * 服务器配置
     */
    @Data
    public static class ServerConfig {
        private String host = "localhost";
        private int port = 8080;
        private String externalUrl = "http://localhost:8080";
        private boolean httpsEnabled = false;
        private String domain;
        private String subPath = "";
        private Flavor flavor = Flavor.ALLINONE;
        private DeploymentType deployment = DeploymentType.SELFHOSTED;
        
        public String getHost() {
            return host;
        }
        
        public int getPort() {
            return port;
        }
        
        public String getExternalUrl() {
            return externalUrl;
        }
        
        public Flavor getFlavor() {
            return flavor;
        }
        
        public DeploymentType getDeployment() {
            return deployment;
        }
    }

    /**
     * 数据库配置
     */
    @Data
    public static class DatabaseConfig {
        private String url = "jdbc:mysql://localhost:3306/affine?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true";
        private String username = "root";
        private String password = "root";
        private String driver = "com.mysql.cj.jdbc.Driver";
        private int maxPoolSize = 10;
        private int minPoolSize = 2;
        private boolean showSql = false;
        private boolean formatSql = false;
        
        public String getUrl() {
            return url;
        }
    }

    /**
     * Redis 配置
     */
    @Data
    public static class RedisConfig {
        private String host = "localhost";
        private int port = 6379;
        private String password;
        private int database = 0;
        private int timeout = 3000;
        private int maxActive = 8;
        private int maxIdle = 8;
        private int minIdle = 0;
        private long maxWait = -1;
        
        public String getHost() {
            return host;
        }
        
        public int getPort() {
            return port;
        }
    }

    /**
     * 存储配置
     */
    @Data
    public static class StorageConfig {
        private StorageProvider provider = StorageProvider.LOCAL;
        private String bucket;
        private String region;
        private String accessKeyId;
        private String secretAccessKey;
        private String endpoint;
        private String publicUrlPrefix;
        private String localPath = "./storage";
        private String serverPath = "/opt/affine/storage";
        private String tempPath = "/tmp/affine-uploads";
        
        /**
         * 获取实际存储路径
         * 根据当前运行环境和配置自动选择路径
         */
        public String getActualStoragePath() {
            // 检测是否为生产环境（Linux服务器）
            if (isLinuxServer()) {
                return serverPath;
            }
            return localPath;
        }
        
        /**
         * 获取临时上传路径
         */
        public String getActualTempPath() {
            if (isLinuxServer()) {
                return tempPath;
            }
            return System.getProperty("java.io.tmpdir") + "/affine-uploads";
        }
        
        /**
         * 检测是否为Linux服务器环境
         */
        private boolean isLinuxServer() {
            String os = System.getProperty("os.name").toLowerCase();
            String userDir = System.getProperty("user.dir");
            
            // 如果是Linux系统且不在用户家目录下运行，认为是服务器环境
            return os.contains("linux") && 
                   !userDir.startsWith("/home/") && 
                   !userDir.contains("Documents");
        }
    }

    /**
     * AI 配置
     */
    @Data
    public static class CopilotConfig {
        private boolean enabled = false;
        private OpenAIConfig openai = new OpenAIConfig();
        private AnthropicConfig anthropic = new AnthropicConfig();
        private GoogleConfig google = new GoogleConfig();
    }

    /**
     * 文档服务配置
     */
    @Data
    public static class DocServiceConfig {
        private boolean enabled = false;
        private String endpoint;
        private String secretKey;
        private int timeout = 30000; // 30秒超时
        private boolean fallbackToDatabase = true;
    }

    /**
     * OpenAI 配置
     */
    @Data
    public static class OpenAIConfig {
        private boolean enabled = false;
        private String apiKey;
        private String baseUrl = "https://api.openai.com/v1";
        private String model = "gpt-3.5-turbo";
    }

    /**
     * Anthropic 配置
     */
    @Data
    public static class AnthropicConfig {
        private boolean enabled = false;
        private String apiKey;
        private String model = "claude-3-sonnet-20240229";
    }

    /**
     * Google 配置
     */
    @Data
    public static class GoogleConfig {
        private boolean enabled = false;
        private String apiKey;
        private String model = "gemini-pro";
    }

    /**
     * 支付配置
     */
    @Data
    public static class PaymentConfig {
        private boolean enabled = false;
        private StripeConfig stripe = new StripeConfig();
    }

    /**
     * Stripe 配置
     */
    @Data
    public static class StripeConfig {
        private String secretKey;
        private String publicKey;
        private String webhookSecret;
        private String priceId;
    }

    /**
     * 认证配置
     */
    @Data
    public static class AuthConfig {
        private JwtConfig jwt = new JwtConfig();
        private SessionConfig session = new SessionConfig();
        private OAuthConfig oauth = new OAuthConfig();
    }

    /**
     * JWT 配置
     */
    @Data
    public static class JwtConfig {
        private String secret = "affine_jwt_secret_key_please_change_in_production";
        private long expiration = 604800000L; // 7 days
        private long refreshExpiration = 2592000000L; // 30 days
        private String issuer = "affine";
    }

    /**
     * Session 配置
     */
    @Data
    public static class SessionConfig {
        private long maxAge = 604800000; // 7 days
        private String cookieName = "affine-session";
        private boolean secure = false;
        private boolean httpOnly = true;
        private String sameSite = "lax";
    }

    /**
     * OAuth 配置
     */
    @Data
    public static class OAuthConfig {
        private GoogleOAuthConfig google = new GoogleOAuthConfig();
        private GitHubOAuthConfig github = new GitHubOAuthConfig();
    }

    /**
     * Google OAuth 配置
     */
    @Data
    public static class GoogleOAuthConfig {
        private boolean enabled = false;
        private String clientId;
        private String clientSecret;
    }

    /**
     * GitHub OAuth 配置
     */
    @Data
    public static class GitHubOAuthConfig {
        private boolean enabled = false;
        private String clientId;
        private String clientSecret;
    }

    /**
     * 通知配置
     */
    @Data
    public static class NotificationConfig {
        private EmailConfig email = new EmailConfig();
    }

    /**
     * 邮件配置
     */
    @Data
    public static class EmailConfig {
        private boolean enabled = false;
        private String host;
        private int port = 587;
        private String username;
        private String password;
        private String from;
        private boolean auth = true;
        private boolean starttls = true;
    }

    /**
     * 服务类型枚举
     */
    public enum Flavor {
        ALLINONE,
        GRAPHQL,
        SYNC,
        RENDERER,
        DOC,
        SCRIPT
    }

    /**
     * 部署类型枚举
     */
    public enum DeploymentType {
        AFFINE,
        SELFHOSTED
    }

    /**
     * 存储提供商枚举
     */
    public enum StorageProvider {
        LOCAL,
        S3,
        R2,
        COS
    }
}
