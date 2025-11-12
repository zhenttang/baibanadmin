package com.yunke.backend.workspace.service.impl;

import com.yunke.backend.workspace.domain.entity.WorkspaceFeature;
import com.yunke.backend.system.domain.entity.Feature;

import com.yunke.backend.workspace.repository.WorkspaceFeatureRepository;
import com.yunke.backend.workspace.service.WorkspaceFeatureService;
import com.yunke.backend.system.service.FeatureService;
import com.yunke.backend.workspace.dto.WorkspaceQuotaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.HashSet;

/**
 * 工作空间特性服务实现 - 完全按照AFFiNE的WorkspaceFeatureModel实现
 * 对应: /packages/backend/server/src/models/workspace-feature.ts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceFeatureServiceImpl implements WorkspaceFeatureService {

    private final WorkspaceFeatureRepository workspaceFeatureRepository;
    private final FeatureService featureService;

    // 对应: async get<T extends WorkspaceFeatureName>(workspaceId: string, name: T)
    @Override
    public Mono<Feature> get(String workspaceId, String name) {
        return Mono.fromCallable(() -> workspaceFeatureRepository.existsByWorkspaceIdAndNameAndActivatedTrue(workspaceId, name))
            .flatMap(exists -> {
                if (!exists) {
                    return Mono.empty();
                }
                return featureService.get(name);
            });
    }

    // 对应: async getQuota(workspaceId: string)
    @Override
    public Mono<WorkspaceQuotaDto> getQuota(String workspaceId) {
        return Mono.fromCallable(() -> 
            workspaceFeatureRepository.findByWorkspaceIdAndTypeAndActivatedTrue(workspaceId, Feature.FeatureType.QUOTA.getValue())
        )
        .flatMap(workspaceFeatures -> {
            if (workspaceFeatures.isEmpty()) {
                return Mono.empty();
            }
            
            // 获取第一个配额特性
            WorkspaceFeature workspaceFeature = workspaceFeatures.get(0);
            
            return featureService.get(workspaceFeature.getName())
                .map(feature -> {
                    // 将Feature转换为WorkspaceQuotaDto
                    Map<String, Object> configs = feature.getConfigs();
                    return WorkspaceQuotaDto.builder()
                        .name((String) configs.get("name"))
                        .blobLimit(((Number) configs.get("blobLimit")).longValue())
                        .storageQuota(((Number) configs.get("storageQuota")).longValue())
                        .historyPeriod(((Number) configs.get("historyPeriod")).intValue())
                        .memberLimit(((Number) configs.get("memberLimit")).intValue())
                        .copilotActionLimit(
                            configs.containsKey("copilotActionLimit") 
                                ? ((Number) configs.get("copilotActionLimit")).intValue()
                                : null
                        )
                        .build();
                });
        });
    }

    // 对应: async has(workspaceId: string, name: WorkspaceFeatureName)
    @Override
    public Mono<Boolean> has(String workspaceId, String name) {
        return Mono.fromCallable(() -> 
            workspaceFeatureRepository.existsByWorkspaceIdAndNameAndActivatedTrue(workspaceId, name)
        );
    }

    // 对应: async batchHasQuota(workspaceIds: string[])
    // 这是性能关键方法，用于批量查询优化
    @Override
    public Mono<Set<String>> batchHasQuota(List<String> workspaceIds) {
        if (workspaceIds.isEmpty()) {
            return Mono.just(new HashSet<>());
        }

        return Mono.fromCallable(() -> {
            // 使用新的批量查询方法
            List<String> workspacesWithQuota = workspaceFeatureRepository.findWorkspaceIdsWithQuotaFeatures(workspaceIds);
            Set<String> result = new HashSet<>(workspacesWithQuota);
            
            log.debug("Batch quota check: {} out of {} workspaces have quota features", 
                result.size(), workspaceIds.size());
                
            return result;
        });
    }

    // 对应: async list(workspaceId: string, type?: FeatureType)
    @Override
    public Mono<List<String>> list(String workspaceId, Integer type) {
        return Mono.fromCallable(() -> {
            if (type == null) {
                return workspaceFeatureRepository.findByWorkspaceIdAndActivatedTrue(workspaceId)
                    .stream()
                    .map(WorkspaceFeature::getName)
                    .collect(Collectors.toList());
            } else {
                return workspaceFeatureRepository.findByWorkspaceIdAndTypeAndActivatedTrue(workspaceId, type)
                    .stream()
                    .map(WorkspaceFeature::getName)
                    .collect(Collectors.toList());
            }
        });
    }

    // 对应: async add(workspaceId: string, name: WorkspaceFeatureName, reason: string)
    @Override
    @Transactional
    public Mono<WorkspaceFeature> add(String workspaceId, String name, String reason) {
        return featureService.get(name)
            .flatMap(feature -> {
                // 检查是否已存在
                return Mono.fromCallable(() -> 
                    workspaceFeatureRepository.findByWorkspaceIdAndName(workspaceId, name)
                )
                .flatMap(existingOpt -> {
                    if (existingOpt.isPresent()) {
                        // 如果已存在，返回现有的
                        return Mono.just(existingOpt.get());
                    } else {
                        // 创建新的工作空间特性记录
                        return Mono.fromCallable(() -> {
                            WorkspaceFeature workspaceFeature = WorkspaceFeature.builder()
                                .workspaceId(workspaceId)
                                .featureId(feature.getId())
                                .name(name)
                                .type(featureService.getFeatureType(name))
                                .configs(feature.getConfigs())
                                .activated(true)
                                .reason(reason)
                                .build();
                            
                            return workspaceFeatureRepository.save(workspaceFeature);
                        })
                        .doOnSuccess(saved -> 
                            log.info("Added feature {} to workspace {}", name, workspaceId)
                        );
                    }
                });
            });
    }

    // 对应: async remove(workspaceId: string, featureName: WorkspaceFeatureName)
    @Override
    @Transactional
    public Mono<Void> remove(String workspaceId, String featureName) {
        return Mono.fromRunnable(() -> 
            workspaceFeatureRepository.deleteByWorkspaceIdAndName(workspaceId, featureName)
        )
        .doOnSuccess(v -> 
            log.info("Removed feature {} from workspace {}", featureName, workspaceId)
        )
        .then();
    }

    // 对应: async setup(workspaceId: string, featureName: string)
    // 这是QuotaService中使用的便捷方法
    @Override
    @Transactional
    public Mono<WorkspaceFeature> setup(String workspaceId, String featureName) {
        return add(workspaceId, featureName, "System setup");
    }
}