package com.yunke.backend.forum.repository;

import com.yunke.backend.forum.domain.entity.ForumReply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ForumReplyRepository extends JpaRepository<ForumReply, Long> {
    
    Page<ForumReply> findByPostIdAndStatusOrderByFloorAsc(
        String postId, 
        String status, 
        Pageable pageable
    );
    
    // Simple listing without status filter
    Page<ForumReply> findByPostIdOrderByFloorAsc(String postId, Pageable pageable);
    
    Page<ForumReply> findByUserIdOrderByCreatedAtDesc(String userId, Pageable pageable);
    
    @Query("SELECT MAX(r.floor) FROM ForumReply r WHERE r.postId = :postId")
    Integer findMaxFloorByPostId(@Param("postId") String postId);
    
    long countByPostIdAndStatus(String postId, String status);
    
    // Find current best answer for a post
    Optional<ForumReply> findByPostIdAndIsBestAnswerTrue(String postId);
}
