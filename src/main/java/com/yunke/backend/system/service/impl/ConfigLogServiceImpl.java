package com.yunke.backend.system.service.impl;

import com.yunke.backend.system.dto.ConfigOperationLogDto;
import com.yunke.backend.system.domain.entity.ConfigOperationLog;
import com.yunke.backend.system.repository.ConfigOperationLogRepository;
import com.yunke.backend.system.service.ConfigLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 配置日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConfigLogServiceImpl implements ConfigLogService {
    
    private final ConfigOperationLogRepository logRepository;
    
    @Override
    @Transactional
    public void logOperation(String operationType, String moduleName, String configKey, 
                           String oldValue, String newValue, String operator, 
                           String sourceIp, String description) {
        try {
            ConfigOperationLog logEntry = ConfigOperationLog.builder()
                    .operationType(operationType)
                    .moduleName(moduleName)
                    .configKey(configKey)
                    .oldValue(oldValue)
                    .newValue(newValue)
                    .operator(operator)
                    .sourceIp(sourceIp)
                    .description(description)
                    .result("SUCCESS")
                    .build();
            
            logRepository.save(logEntry);
            log.info("配置操作日志已记录: {}|{}|{}|{}", operationType, moduleName, configKey, operator);
            
        } catch (Exception e) {
            log.error("记录配置操作日志失败", e);
            // 日志记录失败不应该影响主要操作，所以不抛出异常
        }
    }
    
    @Override
    @Transactional
    public void logFailedOperation(String operationType, String moduleName, String configKey,
                                 String operator, String sourceIp, String errorMessage) {
        try {
            ConfigOperationLog logEntry = ConfigOperationLog.builder()
                    .operationType(operationType)
                    .moduleName(moduleName)
                    .configKey(configKey)
                    .operator(operator)
                    .sourceIp(sourceIp)
                    .result("FAILED")
                    .errorMessage(errorMessage)
                    .build();
            
            logRepository.save(logEntry);
            log.warn("配置操作失败日志已记录: {}|{}|{}|{}", operationType, moduleName, configKey, operator);
            
        } catch (Exception e) {
            log.error("记录配置操作失败日志失败", e);
        }
    }
    
    @Override
    public List<ConfigOperationLogDto> getRecentLogs(String moduleName, Integer limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit != null ? limit : 50);
            Page<ConfigOperationLog> logs = logRepository.findRecentLogs(moduleName, pageable);
            
            return logs.getContent().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("获取配置操作日志失败", e);
            return List.of();
        }
    }
    
    @Override
    public List<ConfigOperationLogDto> getLogsByOperator(String operator, Integer limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit != null ? limit : 50);
            Page<ConfigOperationLog> logs = logRepository.findByOperatorOrderByOperationTimeDesc(operator, pageable);
            
            return logs.getContent().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("获取操作用户日志失败", e);
            return List.of();
        }
    }
    
    @Override
    public List<ConfigOperationLogDto> getLogsByTimeRange(LocalDateTime startTime, LocalDateTime endTime, Integer limit) {
        try {
            Pageable pageable = PageRequest.of(0, limit != null ? limit : 100);
            Page<ConfigOperationLog> logs = logRepository.findByOperationTimeBetween(startTime, endTime, pageable);
            
            return logs.getContent().stream()
                    .map(this::convertToDto)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("获取时间范围日志失败", e);
            return List.of();
        }
    }
    
    @Override
    public long getOperationCount(LocalDateTime startTime, LocalDateTime endTime) {
        try {
            return logRepository.countOperationsBetween(startTime, endTime);
        } catch (Exception e) {
            log.error("统计操作次数失败", e);
            return 0;
        }
    }
    
    @Override
    @Transactional
    public void cleanupOldLogs(LocalDateTime cutoffTime) {
        try {
            logRepository.deleteByOperationTimeBefore(cutoffTime);
            log.info("清理了{}之前的配置操作日志", cutoffTime);
        } catch (Exception e) {
            log.error("清理旧配置日志失败", e);
            throw new RuntimeException("清理旧配置日志失败: " + e.getMessage());
        }
    }
    
    private ConfigOperationLogDto convertToDto(ConfigOperationLog log) {
        return ConfigOperationLogDto.builder()
                .id(log.getId().toString())
                .operationType(log.getOperationType())
                .moduleName(log.getModuleName())
                .configKey(log.getConfigKey())
                .oldValue(log.getOldValue())
                .newValue(log.getNewValue())
                .operator(log.getOperator())
                .operationTime(log.getOperationTime())
                .sourceIp(log.getSourceIp())
                .description(log.getDescription())
                .result(log.getResult())
                .build();
    }
}