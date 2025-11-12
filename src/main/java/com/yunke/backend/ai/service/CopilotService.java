package com.yunke.backend.ai.service;

import com.yunke.backend.ai.dto.CreateChatSessionInput;
import com.yunke.backend.ai.dto.CopilotSessionDto;
import com.yunke.backend.ai.dto.CreateChatMessageInput;
import com.yunke.backend.ai.dto.CopilotMessageDto;
import com.yunke.backend.ai.dto.CopilotQuotaDto;
import com.yunke.backend.ai.domain.entity.CopilotQuota;
import com.yunke.backend.ai.domain.entity.CopilotSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Copilot服务接口
 * 对应Node.js版本的CopilotService
 */
public interface CopilotService {

    // ==================== 会话管理 ====================

    /**
     * 创建聊天会话
     */
    Mono<CopilotSessionDto> createSession(CreateChatSessionInput input, String userId);

    /**
     * 获取会话详情
     */
    Mono<CopilotSessionDto> getSession(String sessionId, String userId);

    /**
     * 获取用户的会话列表
     */
    Mono<List<CopilotSessionDto>> getUserSessions(String userId, int limit);

    /**
     * 获取工作空间的会话列表
     */
    Mono<List<CopilotSessionDto>> getWorkspaceSessions(String workspaceId, String userId, int limit);

    /**
     * 结束会话
     */
    Mono<Boolean> finishSession(String sessionId, String userId);

    /**
     * 删除会话
     */
    Mono<Boolean> deleteSession(String sessionId, String userId);

    /**
     * 清理用户的所有会话
     */
    Mono<Integer> cleanupUserSessions(String userId);

    // ==================== 消息管理 ====================

    /**
     * 发送消息
     */
    Mono<CopilotMessageDto> sendMessage(CreateChatMessageInput input, String userId);

    /**
     * 发送流式消息
     */
    Flux<CopilotMessageDto> sendStreamMessage(CreateChatMessageInput input, String userId);

    /**
     * 获取会话消息
     */
    Mono<List<CopilotMessageDto>> getSessionMessages(String sessionId, String userId, int limit, int offset);

    /**
     * 删除消息
     */
    Mono<Boolean> deleteMessage(String messageId, String userId);

    /**
     * 搜索消息
     */
    Mono<List<CopilotMessageDto>> searchMessages(String sessionId, String query, String userId);

    // ==================== 配额管理 ====================

    /**
     * 获取用户配额
     */
    Mono<CopilotQuotaDto> getUserQuota(String userId, CopilotQuota.CopilotFeature feature);

    /**
     * 获取工作空间配额
     */
    Mono<CopilotQuotaDto> getWorkspaceQuota(String workspaceId, CopilotQuota.CopilotFeature feature);

    /**
     * 检查配额是否可用
     */
    Mono<Boolean> checkQuotaAvailable(String userId, String workspaceId, CopilotQuota.CopilotFeature feature);

    /**
     * 消耗配额
     */
    Mono<Void> consumeQuota(String userId, String workspaceId, CopilotQuota.CopilotFeature feature, int tokens);

    /**
     * 获取用户所有配额
     */
    Mono<List<CopilotQuotaDto>> getAllUserQuotas(String userId);

    /**
     * 获取工作空间所有配额
     */
    Mono<List<CopilotQuotaDto>> getAllWorkspaceQuotas(String workspaceId);

    /**
     * 重置每日配额
     */
    Mono<Integer> resetDailyQuotas();

    /**
     * 重置每月配额
     */
    Mono<Integer> resetMonthlyQuotas();

    // ==================== AI提供商管理 ====================

    /**
     * 获取可用的AI提供商
     */
    Mono<List<String>> getAvailableProviders();

    /**
     * 获取提供商支持的模型
     */
    Mono<List<String>> getProviderModels(CopilotSession.AIProvider provider);

    /**
     * 验证提供商配置
     */
    Mono<Boolean> validateProviderConfig(CopilotSession.AIProvider provider);

    // ==================== 统计和监控 ====================

    /**
     * 获取用户使用统计
     */
    Mono<UsageStatsDto> getUserUsageStats(String userId);

    /**
     * 获取工作空间使用统计
     */
    Mono<UsageStatsDto> getWorkspaceUsageStats(String workspaceId);

    /**
     * 获取会话统计
     */
    Mono<SessionStatsDto> getSessionStats(String sessionId, String userId);

    /**
     * 使用统计DTO
     */
    record UsageStatsDto(
            Long totalSessions,
            Long totalMessages,
            Long totalTokens,
            Long todaySessions,
            Long todayMessages,
            Long todayTokens
    ) {}

    /**
     * 会话统计DTO
     */
    record SessionStatsDto(
            String sessionId,
            Integer messageCount,
            Integer totalTokens,
            Integer userMessages,
            Integer assistantMessages,
            String status
    ) {}
}