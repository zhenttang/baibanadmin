package com.yunke.backend.forum.repository;

import com.yunke.backend.forum.domain.entity.Forum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ForumRepository extends JpaRepository<Forum, Long> {
    
    Optional<Forum> findBySlug(String slug);
    
    List<Forum> findByParentIdOrderByDisplayOrderAsc(Long parentId);
    
    List<Forum> findByIsActiveTrueOrderByDisplayOrderAsc();
    
    @Query("SELECT f FROM Forum f WHERE f.parentId IS NULL ORDER BY f.displayOrder ASC")
    List<Forum> findRootForums();
    
    boolean existsBySlug(String slug);
}
