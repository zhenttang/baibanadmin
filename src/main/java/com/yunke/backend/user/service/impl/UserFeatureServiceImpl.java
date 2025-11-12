package com.yunke.backend.user.service.impl;

import com.yunke.backend.user.domain.entity.UserFeature;
import com.yunke.backend.system.domain.entity.Feature;
import com.yunke.backend.user.repository.UserFeatureRepository;
import com.yunke.backend.user.service.UserFeatureService;
import com.yunke.backend.system.service.FeatureService;
import com.yunke.backend.user.dto.UserQuotaDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

/**
 * 用户特性服务实现 - 完全按照AFFiNE的UserFeatureModel实现
 * 对应: /packages/backend/server/src/models/user-feature.ts
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserFeatureServiceImpl implements UserFeatureService {

    private final UserFeatureRepository userFeatureRepository;
    private final FeatureService featureService;

    // 对应: async get<T extends UserFeatureName>(userId: string, name: T)
    @Override
    public Mono<Feature> get(String userId, String name) {
        return Mono.fromCallable(() -> 
            userFeatureRepository.hasFeature(userId, name)
        )
        .flatMap(hasFeature -> {
            if (!hasFeature) {
                return Mono.empty();
            }
            return featureService.get(name);
        });
    }

    // 对应: async getQuota(userId: string)
    @Override
    public Mono<UserQuotaDto> getQuota(String userId) {
        return Mono.fromCallable(() -> 
            userFeatureRepository.findActiveFeaturesByType(userId, Feature.FeatureType.QUOTA.getValue())
        )
        .flatMap(userFeatures -> {
            if (userFeatures.isEmpty()) {
                return Mono.empty();
            }
            UserFeature userFeature = userFeatures.get(0); // 取第一个配额特性
            return featureService.get(userFeature.getName())
                .map(feature -> {
                    // 将Feature转换为UserQuotaDto
                    Map<String, Object> configs = feature.getConfigs();
                    return UserQuotaDto.builder()
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

    // 对应: async has(userId: string, name: UserFeatureName)
    @Override
    public Mono<Boolean> has(String userId, String name) {
        return Mono.fromCallable(() -> 
            userFeatureRepository.hasFeature(userId, name)
        );
    }

    // 对应: async list(userId: string, type?: FeatureType)
    @Override
    public Mono<List<String>> list(String userId, Integer type) {
        return Mono.fromCallable(() -> {
            if (type == null) {
                return userFeatureRepository.findActiveFeatureNames(userId);
            } else {
                return userFeatureRepository.findActiveFeaturesByType(userId, type)
                    .stream()
                    .map(UserFeature::getName)
                    .toList();
            }
        });
    }

    // 对应: async add(userId: string, name: UserFeatureName, reason: string)
    @Override
    @Transactional
    public Mono<UserFeature> add(String userId, String name, String reason) {
        return featureService.get(name)
            .flatMap(feature -> {
                // 检查是否已存在
                UserFeature existingFeature = userFeatureRepository.findByUserIdAndName(userId, name).orElse(null);
                if (existingFeature != null && existingFeature.isActivated()) {
                    return Mono.just(existingFeature);
                }
                
                // 创建新的用户特性记录
                UserFeature userFeature = UserFeature.builder()
                    .userId(userId)
                    .featureId(feature.getId())
                    .name(name)
                    .type(featureService.getFeatureType(name))
                    .activated(true)
                    .reason(reason)
                    .build();
                
                UserFeature saved = userFeatureRepository.save(userFeature);
                log.info("Added feature {} to user {}", name, userId);
                return Mono.just(saved);
            });
    }

    // 对应: async remove(userId: string, featureName: UserFeatureName)
    @Override
    @Transactional
    public Mono<Void> remove(String userId, String featureName) {
        return Mono.fromRunnable(() -> 
            userFeatureRepository.deleteByUserIdAndName(userId, featureName)
        )
        .doOnSuccess(v -> 
            log.info("Removed feature {} from user {}", featureName, userId)
        )
        .then();
    }

    // 对应: async setup(userId: string, featureName: string)
    // 这是QuotaService中使用的便捷方法
    @Override
    @Transactional
    public Mono<UserFeature> setup(String userId, String featureName) {
        return add(userId, featureName, "System setup");
    }
}