package com.yunke.backend.system.repository;

import com.yunke.backend.system.domain.entity.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 多用户会话存储库
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, String> {

    /**
     * 查找所有过期的会话
     */
    List<Session> findByDeprecatedExpiresAtBefore(LocalDateTime expiryTime);

    /**
     * 查找所有有效的会话
     */
    List<Session> findByDeprecatedExpiresAtAfter(LocalDateTime now);
}