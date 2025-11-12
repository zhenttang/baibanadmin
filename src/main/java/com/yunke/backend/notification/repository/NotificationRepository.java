package com.yunke.backend.notification.repository;

import com.yunke.backend.notification.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * 通知仓库接口
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, String> {

    /**
     * 根据用户ID查询通知列表（分页）
     * @param userId 用户ID
     * @param pageable 分页参数
     * @return 通知列表
     */
    Page<Notification> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);

    /**
     * 根据用户ID查询未读通知数量
     * @param userId 用户ID
     * @return 未读通知数量
     */
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.userId = :userId AND n.read = false")
    long countUnreadByUserId(@Param("userId") String userId);

    /**
     * 标记通知为已读
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @return 更新的行数
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.id = :notificationId AND n.userId = :userId")
    int markAsRead(@Param("notificationId") String notificationId, @Param("userId") String userId);

    /**
     * 标记用户所有通知为已读
     * @param userId 用户ID
     * @return 更新的行数
     */
    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.userId = :userId AND n.read = false")
    int markAllAsRead(@Param("userId") String userId);

    /**
     * 根据用户ID和通知ID查询通知
     * @param id 通知ID
     * @param userId 用户ID
     * @return 通知对象
     */
    Notification findByIdAndUserId(String id, String userId);

    /**
     * 删除用户的通知
     * @param notificationId 通知ID
     * @param userId 用户ID
     * @return 删除的行数
     */
    @Modifying
    @Query("DELETE FROM Notification n WHERE n.id = :notificationId AND n.userId = :userId")
    int deleteByIdAndUserId(@Param("notificationId") String notificationId, @Param("userId") String userId);
}