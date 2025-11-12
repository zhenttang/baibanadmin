package com.yunke.backend.service;

import java.util.Map;

/**
 * 系统监控和性能指标服务接口
 */
public interface MetricsService {

    /**
     * 获取系统指标
     * 包括CPU、内存、磁盘、网络等系统级指标
     */
    Map<String, Object> getSystemMetrics();

    /**
     * 获取应用指标
     * 包括请求量、响应时间、错误率、用户活跃度等应用级指标
     */
    Map<String, Object> getApplicationMetrics();

    /**
     * 获取数据库指标
     * 包括连接池状态、查询性能、慢查询统计等数据库相关指标
     */
    Map<String, Object> getDatabaseMetrics();

    /**
     * 获取历史指标数据
     * @param type 指标类型 (system/application/database)
     * @param timeRange 时间范围 (1h/6h/24h/7d/30d)
     */
    Map<String, Object> getHistoryMetrics(String type, String timeRange);

    /**
     * 获取实时指标汇总
     * 返回各类指标的汇总信息
     */
    Map<String, Object> getMetricsSummary();

    /**
     * 获取健康检查状态
     * 返回各个组件的健康状态
     */
    Map<String, Object> getHealthStatus();

    /**
     * 导出指标数据
     * @param format 导出格式 (json/csv/excel)
     * @param timeRange 时间范围
     */
    Map<String, Object> exportMetrics(String format, String timeRange);

    /**
     * 记录用户操作指标
     * @param operation 操作类型
     * @param userId 用户ID
     */
    void recordUserOperation(String operation, String userId);

    /**
     * 记录API调用指标
     * @param endpoint API端点
     * @param responseTime 响应时间
     * @param statusCode 状态码
     */
    void recordApiCall(String endpoint, long responseTime, int statusCode);

    /**
     * 记录系统事件
     * @param eventType 事件类型
     * @param eventData 事件数据
     */
    void recordSystemEvent(String eventType, Map<String, Object> eventData);
}