package com.yunke.backend.user.repository;

import com.yunke.backend.user.domain.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 用户会话存储库
 */
@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, String> {

    /**
     * 根据用户ID查找会话
     */
    List<UserSession> findByUserId(String userId);

    /**
     * 根据用户ID查找有效会话
     */
    @Query("SELECT s FROM UserSession s WHERE s.userId = :userId AND s.expiresAt > :now")
    List<UserSession> findValidSessionsByUserId(@Param("userId") String userId, @Param("now") Instant now);

    /**
     * 删除过期会话
     */
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.expiresAt < :now")
    int deleteExpiredSessions(@Param("now") Instant now);

    /**
     * 删除用户的所有会话
     */
    @Modifying
    @Query("DELETE FROM UserSession s WHERE s.userId = :userId")
    int deleteAllUserSessions(@Param("userId") String userId);

    /**
     * 检查会话是否有效
     */
    @Query("SELECT s FROM UserSession s WHERE s.id = :sessionId AND s.expiresAt > :now")
    Optional<UserSession> findValidSession(@Param("sessionId") String sessionId, @Param("now") Instant now);
}