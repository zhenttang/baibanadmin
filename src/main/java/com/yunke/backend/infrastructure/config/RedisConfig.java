package com.yunke.backend.infrastructure.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis 配置
 * 对应 Node.js 版本的 ioredis 配置
 */
@Configuration
@EnableCaching
@EnableConfigurationProperties(AffineConfig.class)
@RequiredArgsConstructor
@Slf4j
public class RedisConfig {

    private final AffineConfig affineConfig;

    /**
     * Redis 连接工厂
     */
    @Bean
    @Primary
    public RedisConnectionFactory redisConnectionFactory() {
        AffineConfig.RedisConfig redisConfig = affineConfig.getRedis();
        
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisConfig.getHost());
        config.setPort(redisConfig.getPort());
        config.setDatabase(redisConfig.getDatabase());
        
        if (redisConfig.getPassword() != null && !redisConfig.getPassword().isEmpty()) {
            config.setPassword(redisConfig.getPassword());
        }
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.setValidateConnection(true);
        factory.setShareNativeConnection(false); // 避免连接共享导致的关闭问题
        
        log.info("Configuring Redis connection to {}:{} database {}", 
            redisConfig.getHost(), redisConfig.getPort(), redisConfig.getDatabase());
        
        return factory;
    }

    /**
     * RedisTemplate 配置
     */
    @Bean
    @Primary
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 配置序列化器
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(objectMapper());
        
        // Key 序列化
        template.setKeySerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        
        // Value 序列化
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.setDefaultSerializer(jsonSerializer);
        template.afterPropertiesSet();
        
        return template;
    }

    /**
     * Redisson 客户端配置（用于分布式锁等高级功能）
     */
    @Bean
    public RedissonClient redissonClient() {
        AffineConfig.RedisConfig redisConfig = affineConfig.getRedis();
        
        Config config = new Config();
        String address = String.format("redis://%s:%d", redisConfig.getHost(), redisConfig.getPort());
        
        config.useSingleServer()
                .setAddress(address)
                .setDatabase(redisConfig.getDatabase())
                .setConnectionMinimumIdleSize(2)
                .setConnectionPoolSize(8)
                .setConnectTimeout(redisConfig.getTimeout())
                .setTimeout(redisConfig.getTimeout());
        
        if (redisConfig.getPassword() != null && !redisConfig.getPassword().isEmpty()) {
            config.useSingleServer().setPassword(redisConfig.getPassword());
        }
        
        return Redisson.create(config);
    }

    /**
     * 缓存管理器配置
     */
    @Bean
    @Primary
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofHours(1))
                .serializeKeysWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer(objectMapper())))
                .disableCachingNullValues();

        // 配置不同缓存的TTL
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // 用户缓存 - 30分钟
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(30)));
        
        // 工作空间缓存 - 1小时
        cacheConfigurations.put("workspaces", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // 权限缓存 - 15分钟
        cacheConfigurations.put("permissions", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // 配置缓存 - 24小时
        cacheConfigurations.put("configs", defaultConfig.entryTtl(Duration.ofHours(24)));
        
        // 会话缓存 - 7天
        cacheConfigurations.put("sessions", defaultConfig.entryTtl(Duration.ofDays(7)));
        
        // AI会话缓存 - 1小时
        cacheConfigurations.put("ai-sessions", defaultConfig.entryTtl(Duration.ofHours(1)));
        
        // 文档元数据缓存 - 2小时
        cacheConfigurations.put("docs", defaultConfig.entryTtl(Duration.ofHours(2)));
        
        // GraphQL查询缓存 - 5分钟
        cacheConfigurations.put("graphql", defaultConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * 队列专用 RedisTemplate
     */
    @Bean("queueRedisTemplate")
    public RedisTemplate<String, Object> queueRedisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // 队列使用简单的字符串序列化
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringSerializer);
        template.setValueSerializer(stringSerializer);
        template.setHashKeySerializer(stringSerializer);
        template.setHashValueSerializer(stringSerializer);
        
        template.afterPropertiesSet();
        return template;
    }

    /**
     * ObjectMapper 配置
     */
    private ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        
        // 配置时间模块
        mapper.registerModule(new JavaTimeModule());
        
        // 配置序列化行为
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        mapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        
        // 配置类型信息（用于多态序列化）
        mapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        
        return mapper;
    }

    /**
     * Redis 键前缀配置
     */
    public static class CacheKeys {
        public static final String USER_PREFIX = "user:";
        public static final String WORKSPACE_PREFIX = "workspace:";
        public static final String PERMISSION_PREFIX = "permission:";
        public static final String SESSION_PREFIX = "session:";
        public static final String AI_SESSION_PREFIX = "ai-session:";
        public static final String DOC_PREFIX = "doc:";
        public static final String CONFIG_PREFIX = "config:";
        public static final String LOCK_PREFIX = "lock:";
        public static final String QUEUE_PREFIX = "queue:";
        public static final String NOTIFICATION_PREFIX = "notification:";
        
        // 队列名称
        public static final String COPILOT_QUEUE = "queue:copilot";
        public static final String DOC_QUEUE = "queue:doc";
        public static final String INDEXER_QUEUE = "queue:indexer";
        public static final String NOTIFICATION_QUEUE = "queue:notification";
        public static final String NIGHTLY_QUEUE = "queue:nightly";
    }
}