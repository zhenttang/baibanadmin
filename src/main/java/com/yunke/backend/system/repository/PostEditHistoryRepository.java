package com.yunke.backend.system.repository;

import com.yunke.backend.system.domain.entity.PostEditHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostEditHistoryRepository extends JpaRepository<PostEditHistory, Long> {
    Page<PostEditHistory> findByPostIdOrderByEditedAtDesc(String postId, Pageable pageable);
    long countByPostId(String postId);
}

