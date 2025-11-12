package com.yunke.backend.user.repository;

import com.yunke.backend.user.domain.entity.UserFollow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户关注Repository接口
 */
@Repository
public interface UserFollowRepository extends JpaRepository<UserFollow, Long> {

    /**
     * 根据关注者ID和被关注者ID查找关注记录
     */
    UserFollow findByFollowerIdAndFollowingId(String followerId, String followingId);

    /**
     * 检查是否已关注
     */
    boolean existsByFollowerIdAndFollowingId(String followerId, String followingId);

    /**
     * 根据关注者ID查找所有关注记录
     */
    List<UserFollow> findByFollowerId(String followerId);

    /**
     * 根据被关注者ID查找所有粉丝记录
     */
    List<UserFollow> findByFollowingId(String followingId);

    /**
     * 删除指定关注关系
     */
    void deleteByFollowerIdAndFollowingId(String followerId, String followingId);

    /**
     * 删除用户的所有关注关系
     */
    void deleteByFollowerId(String followerId);

    /**
     * 删除用户的所有被关注关系
     */
    void deleteByFollowingId(String followingId);

    /**
     * 统计关注者的关注数量
     */
    @Query("SELECT COUNT(uf) FROM UserFollow uf WHERE uf.followerId = :followerId")
    Long countByFollowerId(@Param("followerId") String followerId);

    /**
     * 统计被关注者的粉丝数量
     */
    @Query("SELECT COUNT(uf) FROM UserFollow uf WHERE uf.followingId = :followingId")
    Long countByFollowingId(@Param("followingId") String followingId);

    /**
     * 查找用户关注的所有用户ID
     */
    @Query("SELECT uf.followingId FROM UserFollow uf WHERE uf.followerId = :followerId")
    List<String> findFollowingIdsByFollowerId(@Param("followerId") String followerId);

    /**
     * 查找用户的所有粉丝ID
     */
    @Query("SELECT uf.followerId FROM UserFollow uf WHERE uf.followingId = :followingId")
    List<String> findFollowerIdsByFollowingId(@Param("followingId") String followingId);

    /**
     * 查找互相关注的用户（双向关注）
     */
    @Query("SELECT uf1.followingId FROM UserFollow uf1 " +
           "WHERE uf1.followerId = :userId " +
           "AND EXISTS (SELECT 1 FROM UserFollow uf2 WHERE uf2.followerId = uf1.followingId AND uf2.followingId = :userId)")
    List<String> findMutualFollows(@Param("userId") String userId);
}