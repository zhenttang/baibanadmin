package com.yunke.backend.system.service;

import com.yunke.backend.system.dto.ConfigOperationLogDto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 配置日志服务接口
 */
public interface ConfigLogService {
    
    /**
     * 记录配置操作日志
     * @param operationType 操作类型
     * @param moduleName 模块名称
     * @param configKey 配置键
     * @param oldValue 旧值
     * @param newValue 新值
     * @param operator 操作人
     * @param sourceIp 来源IP
     * @param description 描述
     */
    void logOperation(String operationType, String moduleName, String configKey,
                     String oldValue, String newValue, String operator,
                     String sourceIp, String description);
    
    /**
     * 记录配置操作失败日志
     * @param operationType 操作类型
     * @param moduleName 模块名称
     * @param configKey 配置键
     * @param operator 操作人
     * @param sourceIp 来源IP
     * @param errorMessage 错误信息
     */
    void logFailedOperation(String operationType, String moduleName, String configKey,
                           String operator, String sourceIp, String errorMessage);
    
    /**
     * 获取最近的配置操作日志
     * @param moduleName 模块名称（可选）
     * @param limit 限制数量
     * @return 日志列表
     */
    List<ConfigOperationLogDto> getRecentLogs(String moduleName, Integer limit);
    
    /**
     * 根据操作人获取日志
     * @param operator 操作人
     * @param limit 限制数量
     * @return 日志列表
     */
    List<ConfigOperationLogDto> getLogsByOperator(String operator, Integer limit);
    
    /**
     * 根据时间范围获取日志
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @return 日志列表
     */
    List<ConfigOperationLogDto> getLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Integer limit);
    
    /**
     * 统计指定时间范围内的操作次数
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @return 操作次数
     */
    long getOperationCount(LocalDateTime startTime, LocalDateTime endTime);
    
    /**
     * 清理旧日志
     * @param cutoffTime 截止时间
     */
    void cleanupOldLogs(LocalDateTime cutoffTime);
}