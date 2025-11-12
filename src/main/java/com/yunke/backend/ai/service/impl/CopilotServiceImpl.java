package com.yunke.backend.ai.service.impl;

import com.yunke.backend.ai.AIProvider;
import com.yunke.backend.ai.AIProviderManager;
import com.yunke.backend.ai.dto.*;
import com.yunke.backend.ai.repository.CopilotQuotaRepository;
import com.yunke.backend.security.service.PermissionService;
import com.yunke.backend.system.dto.*;
import com.yunke.backend.ai.domain.entity.CopilotMessage;
import com.yunke.backend.ai.domain.entity.CopilotQuota;
import com.yunke.backend.ai.domain.entity.CopilotSession;
import com.yunke.backend.ai.repository.CopilotMessageRepository;

import com.yunke.backend.ai.repository.CopilotSessionRepository;
import com.yunke.backend.ai.service.CopilotService;
import com.yunke.backend.common.exception.ResourceNotFoundException;
import com.yunke.backend.common.exception.PermissionDeniedException;
import com.yunke.backend.security.util.PermissionUtils;
import com.yunke.backend.security.constants.PermissionActions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.yunke.backend.workspace.repository.WorkspaceDocRepository;
import java.util.Optional;

/**
 * Copilot服务实现
 * 对应Node.js版本的CopilotService
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CopilotServiceImpl implements CopilotService {

    private final CopilotSessionRepository sessionRepository;
    private final CopilotMessageRepository messageRepository;
    private final CopilotQuotaRepository quotaRepository;
    private final AIProviderManager aiProviderManager;
    private final PermissionService permissionService;
    private final WorkspaceDocRepository workspaceDocRepository;

    // ==================== 会话管理 ====================

    @Override
    @Transactional
    public Mono<CopilotSessionDto> createSession(CreateChatSessionInput input, String userId) {
        return validateSessionCreation(input, userId)
                .flatMap(valid -> {
                    if (!valid) {
                        return Mono.error(new RuntimeException("Invalid session creation request"));
                    }

                    CopilotSession session = CopilotSession.builder()
                            .sessionId(UUID.randomUUID().toString())
                            .userId(userId)
                            .workspaceId(input.getWorkspaceId())
                            .docId(input.getDocId())
                            .title(input.getTitle())
                            .provider(input.getProvider() != null ? input.getProvider() : CopilotSession.AIProvider.OPENAI)
                            .model(input.getModel() != null ? input.getModel() : "deepseek-chat")
                            .prompt(input.getPrompt() != null ? input.getPrompt() : "")
                            .status(CopilotSession.SessionStatus.ACTIVE)
                            .tokensUsed(0)
                            .messageCount(0)
                            .config(input.getConfig())
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();

                    return Mono.fromCallable(() -> sessionRepository.save(session))
                            .subscribeOn(Schedulers.boundedElastic())
                            .map(this::toSessionDto);
                })
                .doOnSuccess(session -> log.info("Created Copilot session: {} for user: {}", session.getSessionId(), userId))
                .doOnError(error -> log.error("Failed to create Copilot session for user: {}", userId, error));
    }

    @Override
    public Mono<CopilotSessionDto> getSession(String sessionId, String userId) {
        return Mono.fromCallable(() -> sessionRepository.findById(sessionId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(sessionOpt -> {
                    if (sessionOpt.isEmpty()) {
                        return Mono.error(new ResourceNotFoundException("CopilotSession", sessionId));
                    }

                    CopilotSession session = sessionOpt.get();
                    if (!session.getUserId().equals(userId)) {
                        return Mono.error(new PermissionDeniedException(userId, "session", PermissionActions.READ));
                    }

                    return Mono.just(toSessionDto(session));
                });
    }

    @Override
    public Mono<List<CopilotSessionDto>> getUserSessions(String userId, int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return Mono.fromCallable(() -> sessionRepository.findRecentSessionsByUserId(userId, pageable))
                .map(sessions -> sessions.stream()
                        .map(this::toSessionDto)
                        .collect(Collectors.toList()));
    }

    @Override
    public Mono<List<CopilotSessionDto>> getWorkspaceSessions(String workspaceId, String userId, int limit) {
        return PermissionUtils.requireWorkspacePermission(permissionService, workspaceId, userId, 
                PermissionActions.WORKSPACE_READ,
                () -> Mono.fromCallable(() -> sessionRepository.findByUserIdAndWorkspaceId(userId, workspaceId))
                        .subscribeOn(Schedulers.boundedElastic())
                        .map(sessions -> sessions.stream()
                                        .limit(limit)
                                        .map(this::toSessionDto)
                                        .collect(Collectors.toList()))
                );
    }

    @Override
    @Transactional
    public Mono<Boolean> finishSession(String sessionId, String userId) {
        return Mono.fromCallable(() -> sessionRepository.findById(sessionId))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(sessionOpt -> {
                    if (sessionOpt.isEmpty()) {
                        return Mono.error(new ResourceNotFoundException("CopilotSession", sessionId));
                    }

                    CopilotSession session = sessionOpt.get();
                    if (!session.getUserId().equals(userId)) {
                        return Mono.error(new PermissionDeniedException(userId, "session", PermissionActions.READ));
                    }

                    session.markFinished();
                    sessionRepository.save(session);
                    
                    log.info("Finished Copilot session: {} for user: {}", sessionId, userId);
                    return Mono.just(true);
                });
    }

    @Override
    @Transactional
    public Mono<Boolean> deleteSession(String sessionId, String userId) {
        return Mono.fromCallable(() -> sessionRepository.findById(sessionId))
                .flatMap(sessionOpt -> {
                    if (sessionOpt.isEmpty()) {
                        return Mono.just(false);
                    }

                    CopilotSession session = sessionOpt.get();
                    if (!session.getUserId().equals(userId)) {
                        return Mono.error(new RuntimeException("Access denied to session: " + sessionId));
                    }

                    // 删除会话的所有消息
                    messageRepository.deleteBySessionId(sessionId);
                    
                    // 删除会话
                    sessionRepository.delete(session);
                    
                    log.info("Deleted Copilot session: {} for user: {}", sessionId, userId);
                    return Mono.just(true);
                });
    }

    @Override
    @Transactional
    public Mono<Integer> cleanupUserSessions(String userId) {
        return Mono.fromCallable(() -> {
            List<CopilotSession> userSessions = sessionRepository.findActiveSessionsByUserId(userId);
            
            for (CopilotSession session : userSessions) {
                messageRepository.deleteBySessionId(session.getSessionId());
                sessionRepository.delete(session);
            }
            
            log.info("Cleaned up {} Copilot sessions for user: {}", userSessions.size(), userId);
            return userSessions.size();
        });
    }

    // ==================== 消息管理 ====================

    @Override
    @Transactional
    public Mono<CopilotMessageDto> sendMessage(CreateChatMessageInput input, String userId) {
        return validateMessageCreation(input, userId)
                .flatMap(valid -> {
                    if (!valid) {
                        return Mono.error(new RuntimeException("Invalid message creation request"));
                    }

                    return checkQuotaAvailable(userId, null, CopilotQuota.CopilotFeature.CHAT)
                            .flatMap(hasQuota -> {
                                if (!hasQuota) {
                                    return Mono.error(new RuntimeException("Quota exceeded"));
                                }

                                return processMessage(input, userId);
                            });
                });
    }

    @Override
    public Flux<CopilotMessageDto> sendStreamMessage(CreateChatMessageInput input, String userId) {
        return validateMessageCreation(input, userId)
                .filter(Boolean::booleanValue)
                .switchIfEmpty(Mono.error(new RuntimeException("Invalid message creation request")))
                .flatMapMany(valid -> 
                        checkQuotaAvailable(userId, null, CopilotQuota.CopilotFeature.CHAT)
                                .filter(Boolean::booleanValue)
                                .switchIfEmpty(Mono.error(new RuntimeException("Quota exceeded")))
                                .flatMapMany(hasQuota -> processStreamMessage(input, userId))
                );
    }

    @Override
    public Mono<List<CopilotMessageDto>> getSessionMessages(String sessionId, String userId, int limit, int offset) {
        return validateSessionAccess(sessionId, userId)
                .flatMap(valid -> {
                    if (!valid) {
                        return Mono.error(new RuntimeException("Access denied to session"));
                    }

                    Pageable pageable = PageRequest.of(offset / limit, limit);
                    return Mono.fromCallable(() -> 
                            messageRepository.findBySessionIdWithPagination(sessionId, pageable))
                            .map(messages -> messages.stream()
                                    .map(this::toMessageDto)
                                    .collect(Collectors.toList()));
                });
    }

    @Override
    @Transactional
    public Mono<Boolean> deleteMessage(String messageId, String userId) {
        return Mono.fromCallable(() -> messageRepository.findById(messageId))
                .flatMap(messageOpt -> {
                    if (messageOpt.isEmpty()) {
                        return Mono.just(false);
                    }

                    CopilotMessage message = messageOpt.get();
                    
                    return validateSessionAccess(message.getSessionId(), userId)
                            .flatMap(valid -> {
                                if (!valid) {
                                    return Mono.error(new RuntimeException("Access denied"));
                                }

                                messageRepository.delete(message);
                                log.info("Deleted message: {} from session: {}", messageId, message.getSessionId());
                                return Mono.just(true);
                            });
                });
    }

    @Override
    public Mono<List<CopilotMessageDto>> searchMessages(String sessionId, String query, String userId) {
        return validateSessionAccess(sessionId, userId)
                .flatMap(valid -> {
                    if (!valid) {
                        return Mono.error(new RuntimeException("Access denied to session"));
                    }

                    return Mono.fromCallable(() -> 
                            messageRepository.searchMessagesByContent(sessionId, query))
                            .map(messages -> messages.stream()
                                    .map(this::toMessageDto)
                                    .collect(Collectors.toList()));
                });
    }

    // ==================== 配额管理 ====================

    @Override
    public Mono<CopilotQuotaDto> getUserQuota(String userId, CopilotQuota.CopilotFeature feature) {
        return Mono.fromCallable(() -> 
                quotaRepository.findByUserIdAndFeature(userId, feature)
                        .orElseGet(() -> createDefaultUserQuota(userId, feature))
        ).map(this::toQuotaDto);
    }

    @Override
    public Mono<CopilotQuotaDto> getWorkspaceQuota(String workspaceId, CopilotQuota.CopilotFeature feature) {
        return Mono.fromCallable(() -> 
                quotaRepository.findByWorkspaceIdAndFeature(workspaceId, feature)
                        .orElseGet(() -> createDefaultWorkspaceQuota(workspaceId, feature))
        ).map(this::toQuotaDto);
    }

    @Override
    public Mono<Boolean> checkQuotaAvailable(String userId, String workspaceId, CopilotQuota.CopilotFeature feature) {
        Mono<Boolean> userQuotaCheck = getUserQuota(userId, feature)
                .map(quota -> quota.getCanUse());

        if (workspaceId != null) {
            Mono<Boolean> workspaceQuotaCheck = getWorkspaceQuota(workspaceId, feature)
                    .map(quota -> quota.getCanUse());
            
            return Mono.zip(userQuotaCheck, workspaceQuotaCheck)
                    .map(tuple -> tuple.getT1() && tuple.getT2());
        }

        return userQuotaCheck;
    }

    @Override
    @Transactional
    public Mono<Void> consumeQuota(String userId, String workspaceId, CopilotQuota.CopilotFeature feature, int tokens) {
        return Mono.fromCallable(() -> {
            // 消耗用户配额
            quotaRepository.incrementUserUsage(userId, feature, tokens);
            
            // 如果有工作空间，也消耗工作空间配额  
            if (workspaceId != null) {
                quotaRepository.incrementWorkspaceUsage(workspaceId, feature, tokens);
            }
            
            return null;
        }).then();
    }

    @Override
    public Mono<List<CopilotQuotaDto>> getAllUserQuotas(String userId) {
        return Mono.fromCallable(() -> quotaRepository.findAllByUserId(userId))
                .map(quotas -> quotas.stream()
                        .map(this::toQuotaDto)
                        .collect(Collectors.toList()));
    }

    @Override
    public Mono<List<CopilotQuotaDto>> getAllWorkspaceQuotas(String workspaceId) {
        return Mono.fromCallable(() -> quotaRepository.findAllByWorkspaceId(workspaceId))
                .map(quotas -> quotas.stream()
                        .map(this::toQuotaDto)
                        .collect(Collectors.toList()));
    }

    @Override
    @Transactional
    public Mono<Integer> resetDailyQuotas() {
        return Mono.fromCallable(() -> {
            int count = quotaRepository.resetDailyQuotas();
            log.info("Reset daily quotas for {} records", count);
            return count;
        });
    }

    @Override
    @Transactional
    public Mono<Integer> resetMonthlyQuotas() {
        return Mono.fromCallable(() -> {
            int count = quotaRepository.resetMonthlyQuotas();
            log.info("Reset monthly quotas for {} records", count);
            return count;
        });
    }

    // ==================== AI提供商管理 ====================

    @Override
    public Mono<List<String>> getAvailableProviders() {
        return aiProviderManager.getAvailableProviders()
                .map(providers -> providers.stream()
                        .map(provider -> provider.getProviderType().getValue())
                        .collect(Collectors.toList()));
    }

    @Override
    public Mono<List<String>> getProviderModels(CopilotSession.AIProvider provider) {
        Optional<AIProvider> aiProvider = aiProviderManager.getProvider(provider);
        if (aiProvider.isPresent()) {
            return aiProvider.get().getSupportedModels()
                    .map(models -> models.stream()
                            .map(AIProvider.AIModel::id)
                            .collect(Collectors.toList()));
        } else {
            return Mono.just(List.of());
        }
    }

    @Override
    public Mono<Boolean> validateProviderConfig(CopilotSession.AIProvider provider) {
        return aiProviderManager.getProvider(provider)
                .map(AIProvider::isAvailable)
                .orElse(Mono.just(false));
    }

    // ==================== 统计和监控 ====================

    @Override
    public Mono<UsageStatsDto> getUserUsageStats(String userId) {
        return Mono.fromCallable(() -> {
            Object[] todayStats = sessionRepository.getTodayUsageStats(userId);
            Pageable pageable = PageRequest.of(0, 1000);
            List<CopilotSession> allSessions = sessionRepository.findRecentSessionsByUserId(userId, pageable);
            
            long totalSessions = allSessions.size();
            long totalMessages = allSessions.stream().mapToLong(s -> s.getMessageCount() != null ? s.getMessageCount() : 0).sum();
            long totalTokens = allSessions.stream().mapToLong(s -> s.getTokensUsed() != null ? s.getTokensUsed() : 0).sum();
            
            long todaySessions = todayStats.length > 0 ? ((Number) todayStats[0]).longValue() : 0;
            long todayTokens = todayStats.length > 1 ? ((Number) todayStats[1]).longValue() : 0;
            long todayMessages = todayStats.length > 2 ? ((Number) todayStats[2]).longValue() : 0;
            
            return new UsageStatsDto(totalSessions, totalMessages, totalTokens, 
                    todaySessions, todayMessages, todayTokens);
        });
    }

    @Override
    public Mono<UsageStatsDto> getWorkspaceUsageStats(String workspaceId) {
        return Mono.fromCallable(() -> {
            Object[] todayStats = sessionRepository.getTodayWorkspaceUsageStats(workspaceId);
            
            // 简化实现，实际应该查询工作空间的所有历史数据
            long todaySessions = todayStats.length > 0 ? ((Number) todayStats[0]).longValue() : 0;
            long todayTokens = todayStats.length > 1 ? ((Number) todayStats[1]).longValue() : 0;
            long todayMessages = todayStats.length > 2 ? ((Number) todayStats[2]).longValue() : 0;
            
            return new UsageStatsDto(todaySessions, todayMessages, todayTokens,
                    todaySessions, todayMessages, todayTokens);
        });
    }

    @Override
    public Mono<SessionStatsDto> getSessionStats(String sessionId, String userId) {
        return validateSessionAccess(sessionId, userId)
                .flatMap(valid -> {
                    if (!valid) {
                        return Mono.error(new RuntimeException("Access denied to session"));
                    }

                    return Mono.fromCallable(() -> {
                        Object[] stats = messageRepository.getMessageStats(sessionId);
                        CopilotSession session = sessionRepository.findById(sessionId).orElse(null);
                        
                        if (session == null) {
                            throw new ResourceNotFoundException("CopilotSession", sessionId);
                        }
                        
                        int messageCount = stats.length > 0 ? ((Number) stats[0]).intValue() : 0;
                        int totalTokens = stats.length > 1 ? ((Number) stats[1]).intValue() : 0;
                        int userMessages = stats.length > 2 ? ((Number) stats[2]).intValue() : 0;
                        int assistantMessages = stats.length > 3 ? ((Number) stats[3]).intValue() : 0;
                        
                        return new SessionStatsDto(sessionId, messageCount, totalTokens,
                                userMessages, assistantMessages, session.getStatus().getValue());
                    });
                });
    }

    // ==================== 辅助方法 ====================

    private Mono<Boolean> validateSessionCreation(CreateChatSessionInput input, String userId) {
        if (input.getWorkspaceId() != null) {
            return PermissionUtils.checkWorkspacePermission(permissionService, input.getWorkspaceId(), userId, "Workspace.Copilot");
        }
        return Mono.just(true);
    }

    private Mono<Boolean> validateMessageCreation(CreateChatMessageInput input, String userId) {
        return validateSessionAccess(input.getSessionId(), userId);
    }

    private Mono<Boolean> validateSessionAccess(String sessionId, String userId) {
        return permissionService.hasWorkspacePermission(sessionId, userId)
                .flatMap(hasPermission -> {
                    if (hasPermission) {
                        return Mono.just(true);
                    } else {
                        return Mono.fromCallable(() -> 
                            sessionRepository.findById(sessionId)
                                    .map(session -> session.getUserId().equals(userId))
                                    .orElse(false)
                        );
                    }
                });
    }

    private Mono<CopilotMessageDto> processMessage(CreateChatMessageInput input, String userId) {
        return Mono.fromCallable(() -> {
            log.info("=== processMessage 开始 ===");
            log.info("User ID: {}, Session ID: {}, Content: {}", userId, input.getSessionId(), input.getContent());
            
            // 保存用户消息
            CopilotMessage userMessage = saveUserMessage(input);
            log.info("用户消息已保存: {}", userMessage.getMessageId());
            
            // 获取AI提供商
            CopilotSession session = sessionRepository.findById(input.getSessionId()).orElseThrow();
            log.info("Session信息: provider={}, model={}", session.getProvider(), session.getModel());
            
            AIProvider provider = aiProviderManager.getProvider(session.getProvider()).orElseThrow();
            log.info("AI Provider: {}", provider.getClass().getSimpleName());
            
            // 构建聊天请求
            ChatCompletionRequest request = buildChatRequest(session, input);
            log.info("聊天请求构建完成: model={}, messages={}", request.getModel(), request.getMessages().size());
            
            // 发送AI请求并保存响应
            log.info("=== 开始调用DeepSeek API ===");
            ChatCompletionResponse response = provider.chatCompletion(request).block();
            log.info("=== DeepSeek API调用完成 ===");
            log.info("Response: {}", response);
            
            CopilotMessage assistantMessage = saveAssistantMessage(session, response);
            log.info("助手消息已保存: {}", assistantMessage.getMessageId());
            
            // 更新会话
            updateSessionAfterMessage(session, response);
            log.info("会话已更新");
            
            // 暂时注释掉配额消耗，避免异步事务问题
            // TODO: 重新实现配额消耗逻辑
            
            CopilotMessageDto result = toMessageDto(assistantMessage);
            log.info("=== processMessage 完成 ===");
            return result;
        });
        
        // 注释掉异步配额消耗部分
        /*
        .flatMap(messageDto -> {
            // 从session中获取workspaceId，异步消耗配额
            return Mono.fromCallable(() -> {
                CopilotSession session = sessionRepository.findById(input.getSessionId()).orElse(null);
                return session != null ? session.getWorkspaceId() : null;
            })
            .flatMap(workspaceId -> 
                consumeQuota(userId, workspaceId, CopilotQuota.CopilotFeature.CHAT, 0)
                    .thenReturn(messageDto)
            );
        });
        */
    }

    private Flux<CopilotMessageDto> processStreamMessage(CreateChatMessageInput input, String userId) {
        return Mono.fromCallable(() -> {
            log.info("=== processStreamMessage 开始 ===");
            log.info("User ID: {}, Session ID: {}, Content: {}, Stream: {}", 
                    userId, input.getSessionId(), input.getContent(), input.getStream());
            
            CopilotMessage userMessage = saveUserMessage(input);
            CopilotSession session = sessionRepository.findById(input.getSessionId()).orElseThrow();
            AIProvider provider = aiProviderManager.getProvider(session.getProvider()).orElseThrow();
            
            log.info("Session信息: provider={}, model={}", session.getProvider(), session.getModel());
            log.info("AI Provider: {}", provider.getClass().getSimpleName());
            
            ChatCompletionRequest request = buildChatRequest(session, input);
            log.info("流式聊天请求构建完成: model={}, messages={}, stream={}", 
                    request.getModel(), request.getMessages().size(), request.getStream());
            
            log.info("=== 开始调用DeepSeek 流式API ===");
            return provider.streamChatCompletion(request);
        })
        .flatMapMany(responseFlux -> responseFlux)
        .doOnNext(response -> log.debug("收到流式响应: {}", response))
        .map(response -> {
            // 处理流式响应片段
            String content = extractContentFromResponse(response);
            log.debug("流式响应内容: '{}'", content);
            
            return CopilotMessageDto.builder()
                    .sessionId(input.getSessionId())
                    .role(CopilotMessage.MessageRole.ASSISTANT)
                    .content(content)
                    .createdAt(LocalDateTime.now())
                    .build();
        })
        .doOnError(error -> log.error("流式处理异常", error))
        .doOnComplete(() -> log.info("=== 流式响应完成 ==="));
    }

    private CopilotMessage saveUserMessage(CreateChatMessageInput input) {
        CopilotMessage message = CopilotMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .sessionId(input.getSessionId())
                .role(input.getRole())
                .content(input.getContent())
                .params(input.getParams())
                .createdAt(LocalDateTime.now())
                .build();
        
        return messageRepository.save(message);
    }

    private CopilotMessage saveAssistantMessage(CopilotSession session, ChatCompletionResponse response) {
        String content = extractContentFromResponse(response);
        
        Integer totalTokens = null;
        if (response.getUsage() != null && response.getUsage().getTotalTokens() != null) {
            totalTokens = response.getUsage().getTotalTokens();
        }
        
        CopilotMessage message = CopilotMessage.builder()
                .messageId(UUID.randomUUID().toString())
                .sessionId(session.getSessionId())
                .role(CopilotMessage.MessageRole.ASSISTANT)
                .content(content)
                .tokens(totalTokens)
                .finishReason(extractFinishReason(response))
                .createdAt(LocalDateTime.now())
                .build();
        
        return messageRepository.save(message);
    }

    private void updateSessionAfterMessage(CopilotSession session, ChatCompletionResponse response) {
        session.incrementMessageCount();
        if (response.getUsage() != null && response.getUsage().getTotalTokens() != null) {
            session.incrementTokenUsage(response.getUsage().getTotalTokens());
        }
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
    }

    private ChatCompletionRequest buildChatRequest(CopilotSession session, CreateChatMessageInput input) {
        List<CopilotMessage> recentMessages = messageRepository.findBySessionIdOrderByCreatedAt(session.getSessionId());
        
        List<ChatCompletionRequest.ChatMessage> messages = recentMessages.stream()
                .map(msg -> ChatCompletionRequest.ChatMessage.builder()
                        .role(msg.getRole().getValue())
                        .content(msg.getContent())
                        .build())
                .collect(Collectors.toList());
        
        // 添加当前用户消息到请求中
        messages.add(ChatCompletionRequest.ChatMessage.builder()
                .role(input.getRole().getValue())
                .content(input.getContent())
                .build());
        
        return ChatCompletionRequest.builder()
                .model(session.getModel() != null ? session.getModel() : "deepseek-chat")
                .messages(messages)
                .maxTokens(4096)
                .temperature(0.7)
                .stream(input.getStream() != null ? input.getStream() : false)
                .build();
    }

    private String extractContentFromResponse(ChatCompletionResponse response) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            ChatCompletionResponse.Choice choice = response.getChoices().get(0);
            if (choice.getMessage() != null && choice.getMessage().getContent() != null) {
                return choice.getMessage().getContent();
            }
            if (choice.getDelta() != null && choice.getDelta().getContent() != null) {
                return choice.getDelta().getContent();
            }
        }
        return "";
    }

    private String extractFinishReason(ChatCompletionResponse response) {
        if (response.getChoices() != null && !response.getChoices().isEmpty()) {
            return response.getChoices().get(0).getFinishReason();
        }
        return null;
    }

    private CopilotQuota createDefaultUserQuota(String userId, CopilotQuota.CopilotFeature feature) {
        CopilotQuota quota = CopilotQuota.builder()
                .userId(userId)
                .feature(feature)
                .limitPerDay(100)
                .limitPerMonth(1000)
                .tokenLimitPerDay(10000)
                .tokenLimitPerMonth(100000)
                .usedToday(0)
                .usedThisMonth(0)
                .tokensUsedToday(0)
                .tokensUsedThisMonth(0)
                .lastResetDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        
        return quotaRepository.save(quota);
    }

    private CopilotQuota createDefaultWorkspaceQuota(String workspaceId, CopilotQuota.CopilotFeature feature) {
        CopilotQuota quota = CopilotQuota.builder()
                .workspaceId(workspaceId)
                .feature(feature)
                .limitPerDay(500)
                .limitPerMonth(5000)
                .tokenLimitPerDay(50000)
                .tokenLimitPerMonth(500000)
                .usedToday(0)
                .usedThisMonth(0)
                .tokensUsedToday(0)
                .tokensUsedThisMonth(0)
                .lastResetDate(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        
        return quotaRepository.save(quota);
    }

    private CopilotSessionDto toSessionDto(CopilotSession session) {
        return CopilotSessionDto.builder()
                .sessionId(session.getSessionId())
                .userId(session.getUserId())
                .workspaceId(session.getWorkspaceId())
                .docId(session.getDocId())
                .title(session.getTitle())
                .provider(session.getProvider())
                .model(session.getModel())
                .prompt(session.getPrompt())
                .status(session.getStatus())
                .tokensUsed(session.getTokensUsed())
                .messageCount(session.getMessageCount())
                .config(session.getConfig())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .finishedAt(session.getFinishedAt())
                .build();
    }

    private CopilotMessageDto toMessageDto(CopilotMessage message) {
        return CopilotMessageDto.builder()
                .messageId(message.getMessageId())
                .sessionId(message.getSessionId())
                .role(message.getRole())
                .content(message.getContent())
                .attachments(message.getAttachments())
                .params(message.getParams())
                .tokens(message.getTokens())
                .finishReason(message.getFinishReason())
                .createdAt(message.getCreatedAt())
                .build();
    }

    private CopilotQuotaDto toQuotaDto(CopilotQuota quota) {
        return CopilotQuotaDto.builder()
                .userId(quota.getUserId())
                .workspaceId(quota.getWorkspaceId())
                .feature(quota.getFeature())
                .limitPerDay(quota.getLimitPerDay())
                .limitPerMonth(quota.getLimitPerMonth())
                .usedToday(quota.getUsedToday())
                .usedThisMonth(quota.getUsedThisMonth())
                .tokenLimitPerDay(quota.getTokenLimitPerDay())
                .tokenLimitPerMonth(quota.getTokenLimitPerMonth())
                .tokensUsedToday(quota.getTokensUsedToday())
                .tokensUsedThisMonth(quota.getTokensUsedThisMonth())
                .lastResetDate(quota.getLastResetDate())
                .createdAt(quota.getCreatedAt())
                .updatedAt(quota.getUpdatedAt())
                .canUse(quota.canUseFeature())
                .requestUsagePercent(quota.getRequestUsagePercent())
                .tokenUsagePercent(quota.getTokenUsagePercent())
                .remainingRequests(quota.getRemainingRequests())
                .remainingTokens(quota.getRemainingTokens())
                .build();
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {
        private Integer index;
        private Message message;
        private Delta delta;
        private String finishReason;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Delta {
        private String role;
        private String content;
    }
}