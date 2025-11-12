package com.yunke.backend.forum.repository;

import com.yunke.backend.forum.domain.entity.ForumModerator;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumModeratorRepository extends JpaRepository<ForumModerator, Long> {
    
    List<ForumModerator> findByForumId(Long forumId);
    
    List<ForumModerator> findByUserId(String userId);
    
    Optional<ForumModerator> findByForumIdAndUserId(Long forumId, String userId);
    
    boolean existsByForumIdAndUserId(Long forumId, String userId);
    
    void deleteByForumIdAndUserId(Long forumId, String userId);
}
