package com.yunke.backend.common.util;

import com.yunke.backend.common.exception.ResourceNotFoundException;
import reactor.core.publisher.Mono;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * 响应式编程工具类
 * 
 * <p>提供常用的响应式流处理工具方法，减少重复代码。</p>
 * 
 * @author System
 * @since 2024-12-19
 */
public final class ReactiveUtils {
    
    private ReactiveUtils() {
        // 工具类，禁止实例化
    }
    
    /**
     * 将 Optional 转换为 Mono，如果为空则返回 ResourceNotFoundException
     * 
     * @param <T> 值类型
     * @param optional Optional 值
     * @param resourceType 资源类型（用于异常消息）
     * @param resourceId 资源ID（用于异常消息）
     * @return Mono 如果 Optional 有值，否则 Mono.error(ResourceNotFoundException)
     */
    public static <T> Mono<T> optionalToMono(Optional<T> optional, String resourceType, String resourceId) {
        return optional.map(Mono::just)
                .orElse(Mono.error(new ResourceNotFoundException(resourceType, resourceId)));
    }
    
    /**
     * 将 Optional 转换为 Mono，如果为空则返回 ResourceNotFoundException
     * 
     * @param <T> 值类型
     * @param optional Optional 值
     * @param resourceType 资源类型（用于异常消息）
     * @param resourceId 资源ID（用于异常消息）
     * @param mapper Optional 有值时的转换函数
     * @return Mono 如果 Optional 有值，否则 Mono.error(ResourceNotFoundException)
     */
    public static <T, R> Mono<R> optionalToMono(
            Optional<T> optional, 
            String resourceType, 
            String resourceId,
            Function<T, R> mapper) {
        return optional.map(value -> Mono.just(mapper.apply(value)))
                .orElse(Mono.error(new ResourceNotFoundException(resourceType, resourceId)));
    }
    
    /**
     * 将 Optional 转换为 Mono，如果为空则返回空 Mono
     * 
     * @param <T> 值类型
     * @param optional Optional 值
     * @return Mono 如果 Optional 有值，否则 Mono.empty()
     */
    public static <T> Mono<T> optionalToMonoOrEmpty(Optional<T> optional) {
        return optional.map(Mono::just)
                .orElse(Mono.empty());
    }
    
    /**
     * 将 Optional 转换为 Mono，如果为空则返回默认值
     * 
     * @param <T> 值类型
     * @param optional Optional 值
     * @param defaultValue 默认值
     * @return Mono 如果 Optional 有值，否则 Mono.just(defaultValue)
     */
    public static <T> Mono<T> optionalToMonoOrDefault(Optional<T> optional, T defaultValue) {
        return optional.map(Mono::just)
                .orElse(Mono.just(defaultValue));
    }
    
    /**
     * 处理 Mono<Optional<T>>，如果 Optional 为空则返回 ResourceNotFoundException
     * 
     * @param <T> 值类型
     * @param monoOptional Mono<Optional<T>>
     * @param resourceType 资源类型（用于异常消息）
     * @param resourceId 资源ID（用于异常消息）
     * @return Mono<T> 如果 Optional 有值，否则 Mono.error(ResourceNotFoundException)
     */
    public static <T> Mono<T> flatMapOptional(Mono<Optional<T>> monoOptional, String resourceType, String resourceId) {
        return monoOptional.flatMap(optional -> optionalToMono(optional, resourceType, resourceId));
    }
    
    /**
     * 处理 Mono<Optional<T>>，如果 Optional 为空则返回空 Mono
     * 
     * @param <T> 值类型
     * @param monoOptional Mono<Optional<T>>
     * @return Mono<T> 如果 Optional 有值，否则 Mono.empty()
     */
    public static <T> Mono<T> flatMapOptionalOrEmpty(Mono<Optional<T>> monoOptional) {
        return monoOptional.flatMap(optional -> optionalToMonoOrEmpty(optional));
    }
    
    /**
     * 处理 Mono<Optional<T>>，如果 Optional 为空则返回默认值
     * 
     * @param <T> 值类型
     * @param monoOptional Mono<Optional<T>>
     * @param defaultValue 默认值
     * @return Mono<T> 如果 Optional 有值，否则 Mono.just(defaultValue)
     */
    public static <T> Mono<T> flatMapOptionalOrDefault(Mono<Optional<T>> monoOptional, T defaultValue) {
        return monoOptional.flatMap(optional -> optionalToMonoOrDefault(optional, defaultValue));
    }
    
    /**
     * 如果 Mono 的值为 null，则返回 ResourceNotFoundException
     * 
     * @param <T> 值类型
     * @param mono Mono<T>
     * @param resourceType 资源类型（用于异常消息）
     * @param resourceId 资源ID（用于异常消息）
     * @return Mono<T> 如果值不为 null，否则 Mono.error(ResourceNotFoundException)
     */
    public static <T> Mono<T> requireNotNull(Mono<T> mono, String resourceType, String resourceId) {
        return mono.flatMap(value -> {
            if (value == null) {
                return Mono.error(new ResourceNotFoundException(resourceType, resourceId));
            }
            return Mono.just(value);
        });
    }
    
    /**
     * 如果 Mono 的值为 null，则返回空 Mono
     * 
     * @param <T> 值类型
     * @param mono Mono<T>
     * @return Mono<T> 如果值不为 null，否则 Mono.empty()
     */
    public static <T> Mono<T> filterNotNull(Mono<T> mono) {
        return mono.filter(value -> value != null);
    }
    
    /**
     * 如果 Mono 的值满足条件，则返回该值，否则返回 ResourceNotFoundException
     * 
     * @param <T> 值类型
     * @param mono Mono<T>
     * @param predicate 条件判断
     * @param resourceType 资源类型（用于异常消息）
     * @param resourceId 资源ID（用于异常消息）
     * @return Mono<T> 如果满足条件，否则 Mono.error(ResourceNotFoundException)
     */
    public static <T> Mono<T> requireCondition(Mono<T> mono, Predicate<T> predicate, String resourceType, String resourceId) {
        return mono.flatMap(value -> {
            if (predicate.test(value)) {
                return Mono.just(value);
            }
            return Mono.error(new ResourceNotFoundException(resourceType, resourceId));
        });
    }
    
    /**
     * 将 Mono 的值转换为另一个类型，如果原值为 null 则返回 ResourceNotFoundException
     * 
     * @param <T> 原值类型
     * @param <R> 目标类型
     * @param mono Mono<T>
     * @param mapper 转换函数
     * @param resourceType 资源类型（用于异常消息）
     * @param resourceId 资源ID（用于异常消息）
     * @return Mono<R> 转换后的值，如果原值为 null 则返回 Mono.error(ResourceNotFoundException)
     */
    public static <T, R> Mono<R> mapOrError(Mono<T> mono, Function<T, R> mapper, String resourceType, String resourceId) {
        return mono.flatMap(value -> {
            if (value == null) {
                return Mono.error(new ResourceNotFoundException(resourceType, resourceId));
            }
            return Mono.just(mapper.apply(value));
        });
    }
}

