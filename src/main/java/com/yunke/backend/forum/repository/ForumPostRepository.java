package com.yunke.backend.forum.repository;

import com.yunke.backend.forum.domain.entity.ForumPost;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumPostRepository extends JpaRepository<ForumPost, String> {
    
    Page<ForumPost> findByForumIdAndStatusOrderByIsStickyDescLastReplyAtDesc(
        Long forumId, 
        ForumPost.PostStatus status, 
        Pageable pageable
    );
    
    Page<ForumPost> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    Page<ForumPost> findByIsEssenceTrueAndStatusOrderByCreatedAtDesc(
        ForumPost.PostStatus status, 
        Pageable pageable
    );
    
    @Query("SELECT p FROM ForumPost p WHERE p.status = :status ORDER BY p.hotScore DESC")
    Page<ForumPost> findHotPosts(@Param("status") ForumPost.PostStatus status, Pageable pageable);
    
    @Query("SELECT p FROM ForumPost p WHERE (p.title LIKE %:keyword% OR p.content LIKE %:keyword%) AND p.status = :status")
    Page<ForumPost> searchPosts(
        @Param("keyword") String keyword, 
        @Param("status") ForumPost.PostStatus status, 
        Pageable pageable
    );
    
    long countByForumId(Long forumId);
}
