package com.yunke.backend.system.service.impl;

import com.yunke.backend.system.domain.entity.GanttViewConfig;
import com.yunke.backend.system.domain.entity.GanttTaskDependency;
import com.yunke.backend.system.domain.entity.GanttOperationLog;
import com.yunke.backend.system.repository.GanttViewConfigRepository;
import com.yunke.backend.system.repository.GanttTaskDependencyRepository;
import com.yunke.backend.system.repository.GanttOperationLogRepository;
import com.yunke.backend.system.service.GanttManagementService;
import com.yunke.backend.system.service.GanttManagementService.CreateDependencyRequest;
import com.yunke.backend.system.service.GanttManagementService.GanttStatistics;
import com.yunke.backend.system.service.GanttManagementService.DailyActivityStats;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 甘特图管理服务实现
 * 
 * @author AFFiNE Development Team
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GanttManagementServiceImpl implements GanttManagementService {
    
    private final GanttViewConfigRepository ganttConfigRepository;
    private final GanttTaskDependencyRepository dependencyRepository;
    private final GanttOperationLogRepository operationLogRepository;
    private final ObjectMapper objectMapper;
    
    // ============ 甘特图配置管理 ============
    
    @Override
    public Mono<GanttViewConfig> getGanttConfig(String workspaceId, String docId) {
        return Mono.fromCallable(() -> 
            ganttConfigRepository.findByWorkspaceIdAndDocId(workspaceId, docId)
                .orElse(null)
        )
        .subscribeOn(Schedulers.boundedElastic())
        .doOnNext(config -> {
            if (config != null) {
                log.debug("Found gantt config for workspace: {}, doc: {}", workspaceId, docId);
            } else {
                log.debug("No gantt config found for workspace: {}, doc: {}", workspaceId, docId);
            }
        });
    }
    
    @Override
    @Transactional
    public Mono<GanttViewConfig> createGanttConfig(String workspaceId, String docId, 
                                                   String timelineConfig, String displayConfig, 
                                                   String workingCalendar, String userId) {
        return Mono.fromCallable(() -> {
            // 检查是否已存在配置
            if (ganttConfigRepository.existsByWorkspaceIdAndDocId(workspaceId, docId)) {
                throw new IllegalArgumentException("Gantt config already exists for this document");
            }
            
            // 创建新配置
            GanttViewConfig config = GanttViewConfig.builder()
                .id(UUID.randomUUID().toString())
                .workspaceId(workspaceId)
                .docId(docId)
                .timelineConfig(timelineConfig != null ? timelineConfig : getDefaultTimelineConfig())
                .displayConfig(displayConfig != null ? displayConfig : getDefaultDisplayConfig())
                .workingCalendar(workingCalendar)
                .build();
            
            GanttViewConfig savedConfig = ganttConfigRepository.save(config);
            
            // 记录操作日志 (fire-and-forget)
            logOperation(workspaceId, docId, userId, 
                        GanttOperationLog.OperationType.CONFIG_UPDATE,
                        new ConfigOperationData("create", savedConfig.getId()))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.warn("Failed to log gantt operation", e))
                .subscribe();
            
            log.info("Created gantt config for workspace: {}, doc: {}, user: {}", 
                    workspaceId, docId, userId);
            
            return savedConfig;
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    @Transactional
    public Mono<GanttViewConfig> updateGanttConfig(String workspaceId, String docId, 
                                                   String timelineConfig, String displayConfig, 
                                                   String workingCalendar, String userId) {
        return Mono.fromCallable(() -> {
            GanttViewConfig config = ganttConfigRepository
                .findByWorkspaceIdAndDocId(workspaceId, docId)
                .orElseThrow(() -> new IllegalArgumentException("Gantt config not found"));
            
            // 保存旧配置用于记录变更
            String oldTimelineConfig = config.getTimelineConfig();
            String oldDisplayConfig = config.getDisplayConfig();
            String oldWorkingCalendar = config.getWorkingCalendar();
            
            // 更新配置
            if (timelineConfig != null) {
                config.setTimelineConfig(timelineConfig);
            }
            if (displayConfig != null) {
                config.setDisplayConfig(displayConfig);
            }
            if (workingCalendar != null) {
                config.setWorkingCalendar(workingCalendar);
            }
            
            GanttViewConfig savedConfig = ganttConfigRepository.save(config);
            
            // 记录操作日志
            ConfigUpdateData updateData = new ConfigUpdateData(
                oldTimelineConfig, timelineConfig,
                oldDisplayConfig, displayConfig,
                oldWorkingCalendar, workingCalendar
            );
            
            logOperation(workspaceId, docId, userId, 
                        GanttOperationLog.OperationType.CONFIG_UPDATE, updateData)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.warn("Failed to log gantt operation", e))
                .subscribe();
            
            log.info("Updated gantt config for workspace: {}, doc: {}, user: {}", 
                    workspaceId, docId, userId);
            
            return savedConfig;
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    @Transactional
    public Mono<Void> deleteGanttConfig(String workspaceId, String docId, String userId) {
        return Mono.fromRunnable(() -> {
            GanttViewConfig config = ganttConfigRepository
                .findByWorkspaceIdAndDocId(workspaceId, docId)
                .orElseThrow(() -> new IllegalArgumentException("Gantt config not found"));
            
            // 删除相关的依赖关系
            dependencyRepository.deleteByDocId(docId);
            
            // 删除配置
            ganttConfigRepository.delete(config);
            
            // 记录操作日志 (fire-and-forget)
            logOperation(workspaceId, docId, userId, 
                        GanttOperationLog.OperationType.CONFIG_UPDATE,
                        new ConfigOperationData("delete", config.getId()))
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.warn("Failed to log gantt operation", e))
                .subscribe();
            
            log.info("Deleted gantt config for workspace: {}, doc: {}, user: {}", 
                    workspaceId, docId, userId);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    @Override
    public Flux<GanttViewConfig> getWorkspaceGanttConfigs(String workspaceId) {
        return Mono.fromCallable(() -> ganttConfigRepository.findByWorkspaceId(workspaceId))
            .flatMapMany(Flux::fromIterable)
            .subscribeOn(Schedulers.boundedElastic())
            .doOnComplete(() -> log.debug("Retrieved gantt configs for workspace: {}", workspaceId));
    }
    
    // ============ 任务依赖关系管理 ============
    
    @Override
    public Flux<GanttTaskDependency> getTaskDependencies(String workspaceId, String docId) {
        return Mono.fromCallable(() -> dependencyRepository.findByWorkspaceIdAndDocId(workspaceId, docId))
            .flatMapMany(Flux::fromIterable)
            .subscribeOn(Schedulers.boundedElastic())
            .doOnComplete(() -> 
                log.debug("Retrieved task dependencies for workspace: {}, doc: {}", workspaceId, docId)
            );
    }
    
    @Override
    @Transactional
    public Mono<GanttTaskDependency> createTaskDependency(String workspaceId, String docId, 
                                                          String fromTaskId, String toTaskId, 
                                                          GanttTaskDependency.DependencyType dependencyType,
                                                          Integer lagDays, String userId) {
        return Mono.fromCallable(() -> {
            // 验证输入
            if (fromTaskId.equals(toTaskId)) {
                throw new IllegalArgumentException("Task cannot depend on itself");
            }
            
            // 检查是否已存在相同的依赖关系
            if (dependencyRepository.existsByWorkspaceIdAndDocIdAndFromTaskIdAndToTaskId(
                    workspaceId, docId, fromTaskId, toTaskId)) {
                throw new IllegalArgumentException("Dependency already exists");
            }
            
            // 检查是否会产生循环依赖
            if (dependencyRepository.wouldCreateCircularDependency(
                    workspaceId, docId, fromTaskId, toTaskId)) {
                throw new IllegalArgumentException("Creating this dependency would result in a circular dependency");
            }
            
            // 创建依赖关系
            GanttTaskDependency dependency = GanttTaskDependency.builder()
                .workspaceId(workspaceId)
                .docId(docId)
                .fromTaskId(fromTaskId)
                .toTaskId(toTaskId)
                .dependencyType(dependencyType)
                .lagDays(lagDays != null ? lagDays : 0)
                .isFlexible(true)
                .build();
            
            GanttTaskDependency savedDependency = dependencyRepository.save(dependency);
            
            // 记录操作日志
            DependencyOperationData operationData = new DependencyOperationData(
                "create", savedDependency.getId(), fromTaskId, toTaskId, 
                dependencyType.getValue(), lagDays
            );
            
            logOperation(workspaceId, docId, userId, 
                        GanttOperationLog.OperationType.DEPENDENCY_ADD, operationData)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.warn("Failed to log gantt operation", e))
                .subscribe();
            
            log.info("Created task dependency: {} -> {}, type: {}, workspace: {}, doc: {}", 
                    fromTaskId, toTaskId, dependencyType, workspaceId, docId);
            
            return savedDependency;
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    @Transactional
    public Mono<Void> deleteTaskDependency(Long dependencyId, String userId) {
        return Mono.fromRunnable(() -> {
            GanttTaskDependency dependency = dependencyRepository.findById(dependencyId)
                .orElseThrow(() -> new IllegalArgumentException("Dependency not found"));
            
            String workspaceId = dependency.getWorkspaceId();
            String docId = dependency.getDocId();
            
            // 删除依赖关系
            dependencyRepository.delete(dependency);
            
            // 记录操作日志
            DependencyOperationData operationData = new DependencyOperationData(
                "delete", dependencyId, dependency.getFromTaskId(), 
                dependency.getToTaskId(), dependency.getDependencyType().getValue(), 
                dependency.getLagDays()
            );
            
            logOperation(workspaceId, docId, userId, 
                        GanttOperationLog.OperationType.DEPENDENCY_REMOVE, operationData)
                .subscribeOn(Schedulers.boundedElastic())
                .doOnError(e -> log.warn("Failed to log gantt operation", e))
                .subscribe();
            
            log.info("Deleted task dependency: {} -> {}, id: {}", 
                    dependency.getFromTaskId(), dependency.getToTaskId(), dependencyId);
        })
        .subscribeOn(Schedulers.boundedElastic())
        .then();
    }
    
    @Override
    @Transactional
    public Flux<GanttTaskDependency> createTaskDependencies(String workspaceId, String docId,
                                                            List<CreateDependencyRequest> requests, 
                                                            String userId) {
        return Flux.fromIterable(requests)
            .flatMap(request -> 
                createTaskDependency(workspaceId, docId, request.fromTaskId(), 
                                   request.toTaskId(), request.dependencyType(), 
                                   request.lagDays(), userId)
            )
            .onErrorContinue((throwable, obj) -> {
                log.warn("Failed to create dependency: {}", throwable.getMessage());
            });
    }
    
    @Override
    public Flux<GanttTaskDependency> getTaskPredecessors(String workspaceId, String docId, String taskId) {
        return Mono.fromCallable(() -> 
            dependencyRepository.findPredecessorsByTask(workspaceId, docId, taskId)
        )
        .flatMapMany(Flux::fromIterable)
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Flux<GanttTaskDependency> getTaskSuccessors(String workspaceId, String docId, String taskId) {
        return Mono.fromCallable(() -> 
            dependencyRepository.findSuccessorsByTask(workspaceId, docId, taskId)
        )
        .flatMapMany(Flux::fromIterable)
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<Boolean> hasCircularDependency(String workspaceId, String docId) {
        return Mono.fromCallable(() -> 
            dependencyRepository.hasCircularDependency(workspaceId, docId)
        )
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Mono<Boolean> wouldCreateCircularDependency(String workspaceId, String docId, 
                                                      String fromTaskId, String toTaskId) {
        return Mono.fromCallable(() -> 
            dependencyRepository.wouldCreateCircularDependency(workspaceId, docId, fromTaskId, toTaskId)
        )
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    // ============ 操作日志管理 ============
    
    @Override
    @Transactional
    public Mono<GanttOperationLog> logOperation(String workspaceId, String docId, String userId,
                                               GanttOperationLog.OperationType operationType, 
                                               Object operationData) {
        return Mono.fromCallable(() -> {
            try {
                String operationDataJson = objectMapper.writeValueAsString(operationData);
                
                GanttOperationLog log = GanttOperationLog.builder()
                    .workspaceId(workspaceId)
                    .docId(docId)
                    .userId(userId)
                    .operationType(operationType)
                    .operationData(operationDataJson)
                    .build();
                
                return operationLogRepository.save(log);
            } catch (Exception e) {
                throw new RuntimeException("Failed to serialize operation data", e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .doOnError(error -> 
            log.error("Failed to log operation: {}", error.getMessage())
        );
    }
    
    @Override
    public Flux<GanttOperationLog> getOperationLogs(String workspaceId, String docId, 
                                                    int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size);
        return Mono.fromCallable(() -> 
            operationLogRepository.findByWorkspaceIdAndDocIdOrderByCreatedAtDesc(
                workspaceId, docId, pageRequest
            ).getContent()
        )
        .flatMapMany(Flux::fromIterable)
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Flux<GanttOperationLog> getUserOperationLogs(String workspaceId, String docId, 
                                                        String userId, int limit) {
        PageRequest pageRequest = PageRequest.of(0, limit);
        return Mono.fromCallable(() -> 
            operationLogRepository.findRecentUserLogs(workspaceId, docId, userId, pageRequest)
        )
        .flatMapMany(Flux::fromIterable)
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    // ============ 统计和分析 ============
    
    @Override
    public Mono<GanttStatistics> getGanttStatistics(String workspaceId, String docId) {
        return Mono.fromCallable(() -> {
            long totalDependencies = dependencyRepository.countByWorkspaceIdAndDocId(workspaceId, docId);
            long flexibleDependencies = dependencyRepository.findFlexibleDependencies(workspaceId, docId).size();
            long rigidDependencies = totalDependencies - flexibleDependencies;
            boolean hasCircularDep = dependencyRepository.hasCircularDependency(workspaceId, docId);
            
            // 获取最后修改信息
            GanttViewConfig config = ganttConfigRepository.findByWorkspaceIdAndDocId(workspaceId, docId)
                .orElse(null);
            
            long totalOperations = 0; // 需要通过其他方法获取，暂时设为0
            
            return new GanttStatistics(
                totalDependencies,
                flexibleDependencies,
                rigidDependencies,
                hasCircularDep,
                totalOperations,
                "unknown", // 需要从最后的操作日志中获取
                config != null ? config.getUpdatedAt() : null
            );
        })
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    @Override
    public Flux<DailyActivityStats> getDailyActivityStats(String workspaceId, String docId, int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        
        return Mono.fromCallable(() -> 
            operationLogRepository.findDailyActivityStats(workspaceId, docId, startDate)
        )
        .flatMapMany(Flux::fromIterable)
        .map(record -> new DailyActivityStats(
            ((java.sql.Date) record[0]).toLocalDate(),
            ((Number) record[1]).longValue(),
            ((Number) record[2]).longValue()
        ))
        .subscribeOn(Schedulers.boundedElastic());
    }
    
    // ============ 私有辅助方法 ============
    
    private String getDefaultTimelineConfig() {
        return """
            {
                "startDate": %d,
                "endDate": %d,
                "unit": "day",
                "showWeekends": true,
                "workingDays": [1, 2, 3, 4, 5]
            }
            """.formatted(
                System.currentTimeMillis(),
                System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000
            );
    }
    
    private String getDefaultDisplayConfig() {
        return """
            {
                "showCriticalPath": false,
                "showProgress": true,
                "compactMode": false
            }
            """;
    }
    
    // ============ 内部数据类 ============
    
    record ConfigOperationData(String action, String configId) {}
    
    record ConfigUpdateData(
        String oldTimelineConfig, String newTimelineConfig,
        String oldDisplayConfig, String newDisplayConfig,
        String oldWorkingCalendar, String newWorkingCalendar
    ) {}
    
    record DependencyOperationData(
        String action, Long dependencyId, String fromTaskId, 
        String toTaskId, String dependencyType, Integer lagDays
    ) {}
}