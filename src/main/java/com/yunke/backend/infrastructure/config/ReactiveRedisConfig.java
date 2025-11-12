package com.yunke.backend.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * 响应式Redis配置
 * 用于实时协作功能
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ReactiveRedisConfig {

    private final AffineConfig affineConfig;

    /**
     * 响应式Redis模板
     */
    @Bean
    public ReactiveRedisTemplate<String, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory,
            ObjectMapper objectMapper) {
        
        // 配置序列化器
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer valueSerializer = new GenericJackson2JsonRedisSerializer(objectMapper);
        
        // 构建序列化上下文
        RedisSerializationContext<String, Object> context = RedisSerializationContext
                .<String, Object>newSerializationContext()
                .key(keySerializer)
                .value(valueSerializer)
                .hashKey(keySerializer)
                .hashValue(valueSerializer)
                .build();
        
        ReactiveRedisTemplate<String, Object> template = new ReactiveRedisTemplate<>(connectionFactory, context);
        
        log.info("Reactive Redis template configured for collaboration features");
        return template;
    }

    /**
     * 响应式Redis连接工厂（复用现有的连接配置）
     */
    @Bean
    public ReactiveRedisConnectionFactory reactiveRedisConnectionFactory() {
        AffineConfig.RedisConfig redisConfig = affineConfig.getRedis();
        
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                redisConfig.getHost(), 
                redisConfig.getPort()
        );
        
        factory.setDatabase(redisConfig.getDatabase());
        factory.setValidateConnection(true);
        
        if (redisConfig.getPassword() != null && !redisConfig.getPassword().isEmpty()) {
            factory.setPassword(redisConfig.getPassword());
        }
        
        factory.afterPropertiesSet();
        
        log.info("Reactive Redis connection factory configured: {}:{}", 
            redisConfig.getHost(), redisConfig.getPort());
        
        return factory;
    }
}