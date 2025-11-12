package com.yunke.backend.user.repository;


import com.yunke.backend.user.domain.entity.UserSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 用户空间文档仓库 - 按照AFFiNE架构设计
 * 
 * 对应AFFiNE中的 UserDocModel
 * 处理用户空间数据的CRUD操作
 */
@Repository
public interface UserSnapshotRepository extends JpaRepository<UserSnapshot, UserSnapshot.UserSnapshotId> {
    
    /**
     * 根据用户ID和文档ID查找用户文档
     * 对应AFFiNE的 get(userId, docId) 方法
     */
    @Query("SELECT u FROM UserSnapshot u WHERE u.userId = :userId AND u.id = :docId")
    Optional<UserSnapshot> findByUserIdAndDocId(@Param("userId") String userId, @Param("docId") String docId);
    
    /**
     * 查找用户的所有文档
     */
    @Query("SELECT u FROM UserSnapshot u WHERE u.userId = :userId ORDER BY u.updatedAt DESC")
    List<UserSnapshot> findAllByUserId(@Param("userId") String userId);
    
    /**
     * 查找用户特定类型的文档
     * 用于查找用户的设置、收藏等特定类型数据
     */
    @Query("SELECT u FROM UserSnapshot u WHERE u.userId = :userId AND u.id LIKE :docIdPattern ORDER BY u.updatedAt DESC")
    List<UserSnapshot> findByUserIdAndDocIdPattern(@Param("userId") String userId, @Param("docIdPattern") String docIdPattern);
    
    /**
     * 统计用户的文档数量
     */
    @Query("SELECT COUNT(u) FROM UserSnapshot u WHERE u.userId = :userId")
    long countByUserId(@Param("userId") String userId);
    
    /**
     * 删除用户的特定文档
     */
    void deleteByUserIdAndId(@Param("userId") String userId, @Param("docId") String docId);
    
    /**
     * 删除用户的所有文档 - 对应AFFiNE的 deleteAllByUserId(userId)
     */
    void deleteByUserId(@Param("userId") String userId);
    
    /**
     * 检查用户文档是否存在
     */
    @Query("SELECT CASE WHEN COUNT(u) > 0 THEN true ELSE false END FROM UserSnapshot u WHERE u.userId = :userId AND u.id = :docId")
    boolean existsByUserIdAndDocId(@Param("userId") String userId, @Param("docId") String docId);
}