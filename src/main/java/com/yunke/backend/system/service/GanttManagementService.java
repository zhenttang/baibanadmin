package com.yunke.backend.system.service;

import com.yunke.backend.system.domain.entity.GanttViewConfig;
import com.yunke.backend.system.domain.entity.GanttTaskDependency;
import com.yunke.backend.system.domain.entity.GanttOperationLog;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 甘特图管理服务接口
 * 
 * @author AFFiNE Development Team
 */
public interface GanttManagementService {
    
    // ============ 甘特图配置管理 ============
    
    /**
     * 获取甘特图配置
     */
    Mono<GanttViewConfig> getGanttConfig(String workspaceId, String docId);
    
    /**
     * 创建甘特图配置
     */
    Mono<GanttViewConfig> createGanttConfig(String workspaceId, String docId, 
                                           String timelineConfig, String displayConfig, 
                                           String workingCalendar, String userId);
    
    /**
     * 更新甘特图配置
     */
    Mono<GanttViewConfig> updateGanttConfig(String workspaceId, String docId, 
                                           String timelineConfig, String displayConfig, 
                                           String workingCalendar, String userId);
    
    /**
     * 删除甘特图配置
     */
    Mono<Void> deleteGanttConfig(String workspaceId, String docId, String userId);
    
    /**
     * 获取工作空间中所有甘特图配置
     */
    Flux<GanttViewConfig> getWorkspaceGanttConfigs(String workspaceId);
    
    // ============ 任务依赖关系管理 ============
    
    /**
     * 获取文档的所有任务依赖关系
     */
    Flux<GanttTaskDependency> getTaskDependencies(String workspaceId, String docId);
    
    /**
     * 创建任务依赖关系
     */
    Mono<GanttTaskDependency> createTaskDependency(String workspaceId, String docId, 
                                                   String fromTaskId, String toTaskId, 
                                                   GanttTaskDependency.DependencyType dependencyType,
                                                   Integer lagDays, String userId);
    
    /**
     * 删除任务依赖关系
     */
    Mono<Void> deleteTaskDependency(Long dependencyId, String userId);
    
    /**
     * 批量创建任务依赖关系
     */
    Flux<GanttTaskDependency> createTaskDependencies(String workspaceId, String docId,
                                                     List<CreateDependencyRequest> requests, 
                                                     String userId);
    
    /**
     * 获取任务的前置依赖
     */
    Flux<GanttTaskDependency> getTaskPredecessors(String workspaceId, String docId, String taskId);
    
    /**
     * 获取任务的后续依赖
     */
    Flux<GanttTaskDependency> getTaskSuccessors(String workspaceId, String docId, String taskId);
    
    /**
     * 检查是否存在循环依赖
     */
    Mono<Boolean> hasCircularDependency(String workspaceId, String docId);
    
    /**
     * 检查添加依赖是否会产生循环
     */
    Mono<Boolean> wouldCreateCircularDependency(String workspaceId, String docId, 
                                               String fromTaskId, String toTaskId);
    
    // ============ 操作日志管理 ============
    
    /**
     * 记录操作日志
     */
    Mono<GanttOperationLog> logOperation(String workspaceId, String docId, String userId,
                                        GanttOperationLog.OperationType operationType, 
                                        Object operationData);
    
    /**
     * 获取操作日志
     */
    Flux<GanttOperationLog> getOperationLogs(String workspaceId, String docId, 
                                             int page, int size);
    
    /**
     * 获取用户的操作日志
     */
    Flux<GanttOperationLog> getUserOperationLogs(String workspaceId, String docId, 
                                                 String userId, int limit);
    
    // ============ 统计和分析 ============
    
    /**
     * 获取甘特图统计信息
     */
    Mono<GanttStatistics> getGanttStatistics(String workspaceId, String docId);
    
    /**
     * 获取协作活动统计
     */
    Flux<DailyActivityStats> getDailyActivityStats(String workspaceId, String docId, int days);
    
    // ============ 数据传输对象 ============
    
    /**
     * 创建依赖关系请求
     */
    record CreateDependencyRequest(
        String fromTaskId,
        String toTaskId,
        GanttTaskDependency.DependencyType dependencyType,
        Integer lagDays,
        Boolean isFlexible
    ) {}
    
    /**
     * 甘特图统计信息
     */
    record GanttStatistics(
        long totalDependencies,
        long flexibleDependencies,
        long rigidDependencies,
        boolean hasCircularDependency,
        long totalOperations,
        String lastModifiedBy,
        java.time.LocalDateTime lastModifiedAt
    ) {}
    
    /**
     * 每日活动统计
     */
    record DailyActivityStats(
        java.time.LocalDate date,
        long operationCount,
        long activeUsers
    ) {}
}