package com.yunke.backend.ai.repository;

import com.yunke.backend.ai.domain.entity.CopilotMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Copilot消息Repository接口
 * 对应Node.js版本的ChatMessageRepository
 */
@Repository
public interface CopilotMessageRepository extends JpaRepository<CopilotMessage, String> {

    /**
     * 根据会话ID查找所有消息
     */
    @Query("SELECT m FROM CopilotMessage m WHERE m.sessionId = :sessionId ORDER BY m.createdAt ASC")
    List<CopilotMessage> findBySessionIdOrderByCreatedAt(@Param("sessionId") String sessionId);

    /**
     * 根据会话ID分页查找消息
     */
    @Query("SELECT m FROM CopilotMessage m WHERE m.sessionId = :sessionId ORDER BY m.createdAt DESC")
    List<CopilotMessage> findBySessionIdWithPagination(@Param("sessionId") String sessionId, Pageable pageable);

    /**
     * 根据会话ID查找最新的消息（列表）
     */
    @Query("SELECT m FROM CopilotMessage m WHERE m.sessionId = :sessionId ORDER BY m.createdAt DESC")
    List<CopilotMessage> findLatestMessageBySessionIdList(@Param("sessionId") String sessionId);
    
    /**
     * 根据会话ID查找最新的消息
     */
    default Optional<CopilotMessage> findLatestMessageBySessionId(String sessionId) {
        List<CopilotMessage> messages = findLatestMessageBySessionIdList(sessionId);
        return messages.isEmpty() ? Optional.empty() : Optional.of(messages.get(0));
    }

    /**
     * 根据会话ID和角色查找消息
     */
    @Query("SELECT m FROM CopilotMessage m WHERE m.sessionId = :sessionId AND m.role = :role ORDER BY m.createdAt ASC")
    List<CopilotMessage> findBySessionIdAndRole(@Param("sessionId") String sessionId, @Param("role") CopilotMessage.MessageRole role);

    /**
     * 统计会话中的消息数量
     */
    @Query("SELECT COUNT(m) FROM CopilotMessage m WHERE m.sessionId = :sessionId")
    long countBySessionId(@Param("sessionId") String sessionId);

    /**
     * 统计会话中各角色的消息数量
     */
    @Query("SELECT m.role, COUNT(m) FROM CopilotMessage m WHERE m.sessionId = :sessionId GROUP BY m.role")
    List<Object[]> countBySessionIdGroupByRole(@Param("sessionId") String sessionId);

    /**
     * 计算会话中的总token使用量
     */
    @Query("SELECT COALESCE(SUM(m.tokens), 0) FROM CopilotMessage m WHERE m.sessionId = :sessionId")
    long sumTokensBySessionId(@Param("sessionId") String sessionId);

    /**
     * 查找包含附件的消息
     */
    @Query("SELECT m FROM CopilotMessage m WHERE m.sessionId = :sessionId AND m.attachments IS NOT NULL ORDER BY m.createdAt DESC")
    List<CopilotMessage> findMessagesWithAttachments(@Param("sessionId") String sessionId);

    /**
     * 根据内容搜索消息
     */
    @Query("SELECT m FROM CopilotMessage m WHERE m.sessionId = :sessionId AND LOWER(m.content) LIKE LOWER(CONCAT('%', :searchTerm, '%')) ORDER BY m.createdAt DESC")
    List<CopilotMessage> searchMessagesByContent(@Param("sessionId") String sessionId, @Param("searchTerm") String searchTerm);

    /**
     * 查找指定时间范围内的消息
     */
    @Query("SELECT m FROM CopilotMessage m WHERE m.sessionId = :sessionId AND m.createdAt BETWEEN :startTime AND :endTime ORDER BY m.createdAt ASC")
    List<CopilotMessage> findBySessionIdAndTimeRange(
            @Param("sessionId") String sessionId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * 删除会话的所有消息
     */
    @Query("DELETE FROM CopilotMessage m WHERE m.sessionId = :sessionId")
    int deleteBySessionId(@Param("sessionId") String sessionId);

    /**
     * 查找最近的用户消息
     */
    @Query("SELECT m FROM CopilotMessage m WHERE m.sessionId = :sessionId AND m.role = 'USER' ORDER BY m.createdAt DESC")
    List<CopilotMessage> findRecentUserMessages(@Param("sessionId") String sessionId, Pageable pageable);

    /**
     * 查找最近的助手回复
     */
    @Query("SELECT m FROM CopilotMessage m WHERE m.sessionId = :sessionId AND m.role = 'ASSISTANT' ORDER BY m.createdAt DESC")
    List<CopilotMessage> findRecentAssistantMessages(@Param("sessionId") String sessionId, Pageable pageable);

    /**
     * 获取会话的消息统计信息
     */
    @Query("SELECT COUNT(m), COALESCE(SUM(m.tokens), 0), " +
           "COUNT(CASE WHEN m.role = 'USER' THEN 1 END), " +
           "COUNT(CASE WHEN m.role = 'ASSISTANT' THEN 1 END) " +
           "FROM CopilotMessage m WHERE m.sessionId = :sessionId")
    Object[] getMessageStats(@Param("sessionId") String sessionId);

    /**
     * 清理旧消息（超过指定天数）
     */
    @Query("DELETE FROM CopilotMessage m WHERE m.createdAt < :cutoffDate")
    int deleteOldMessages(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * 查找错误的消息（没有正确完成的）
     */
    @Query("SELECT m FROM CopilotMessage m WHERE m.sessionId = :sessionId AND m.finishReason IN ('error', 'length', 'content_filter') ORDER BY m.createdAt DESC")
    List<CopilotMessage> findErrorMessages(@Param("sessionId") String sessionId);
}