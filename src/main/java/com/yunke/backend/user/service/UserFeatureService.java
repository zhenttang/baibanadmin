package com.yunke.backend.user.service;

import com.yunke.backend.user.domain.entity.UserFeature;
import com.yunke.backend.system.domain.entity.Feature;
import com.yunke.backend.user.dto.UserQuotaDto;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 用户特性服务接口 - 完全按照AFFiNE的UserFeatureModel实现
 * 对应: /packages/backend/server/src/models/user-feature.ts
 */
public interface UserFeatureService {

    /**
     * 获取用户特性
     * 对应: async get<T extends UserFeatureName>(userId: string, name: T)
     */
    Mono<Feature> get(String userId, String name);

    /**
     * 获取用户配额
     * 对应: async getQuota(userId: string)
     */
    Mono<UserQuotaDto> getQuota(String userId);

    /**
     * 检查特性是否存在
     * 对应: async has(userId: string, name: UserFeatureName)
     */
    Mono<Boolean> has(String userId, String name);

    /**
     * 列出用户特性
     * 对应: async list(userId: string, type?: FeatureType)
     */
    Mono<List<String>> list(String userId, Integer type);

    /**
     * 添加用户特性
     * 对应: async add(userId: string, name: UserFeatureName, reason: string)
     */
    Mono<UserFeature> add(String userId, String name, String reason);

    /**
     * 移除用户特性
     * 对应: async remove(userId: string, featureName: UserFeatureName)
     */
    Mono<Void> remove(String userId, String featureName);

    /**
     * 设置用户特性
     * 便捷方法，用于QuotaService中的特性设置
     */
    Mono<UserFeature> setup(String userId, String featureName);
}