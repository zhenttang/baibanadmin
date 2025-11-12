package com.yunke.backend.workspace.service;

import com.yunke.backend.workspace.domain.entity.WorkspaceFeature;
import com.yunke.backend.system.domain.entity.Feature;
import com.yunke.backend.workspace.dto.WorkspaceQuotaDto;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Set;

/**
 * 工作空间特性服务接口 - 完全按照AFFiNE的WorkspaceFeatureModel实现
 * 对应: /packages/backend/server/src/models/workspace-feature.ts
 */
public interface WorkspaceFeatureService {

    /**
     * 获取工作空间特性
     * 对应: async get<T extends WorkspaceFeatureName>(workspaceId: string, name: T)
     */
    Mono<Feature> get(String workspaceId, String name);

    /**
     * 获取工作空间配额
     * 对应: async getQuota(workspaceId: string)
     */
    Mono<WorkspaceQuotaDto> getQuota(String workspaceId);

    /**
     * 检查特性是否存在
     * 对应: async has(workspaceId: string, name: WorkspaceFeatureName)
     */
    Mono<Boolean> has(String workspaceId, String name);

    /**
     * 批量检查配额特性
     * 对应: async batchHasQuota(workspaceIds: string[])
     * 这是性能关键方法，用于配额计算优化
     */
    Mono<Set<String>> batchHasQuota(List<String> workspaceIds);

    /**
     * 列出工作空间特性
     * 对应: async list(workspaceId: string, type?: FeatureType)
     */
    Mono<List<String>> list(String workspaceId, Integer type);

    /**
     * 添加工作空间特性
     * 对应: async add(workspaceId: string, name: WorkspaceFeatureName, reason: string)
     */
    Mono<WorkspaceFeature> add(String workspaceId, String name, String reason);

    /**
     * 移除工作空间特性
     * 对应: async remove(workspaceId: string, featureName: WorkspaceFeatureName)
     */
    Mono<Void> remove(String workspaceId, String featureName);

    /**
     * 设置工作空间特性
     * 便捷方法，用于QuotaService中的特性设置
     */
    Mono<WorkspaceFeature> setup(String workspaceId, String featureName);
}