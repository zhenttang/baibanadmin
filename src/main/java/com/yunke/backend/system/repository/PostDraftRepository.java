package com.yunke.backend.system.repository;

import com.yunke.backend.system.domain.entity.PostDraft;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostDraftRepository extends JpaRepository<PostDraft, Long> {

    Page<PostDraft> findByUserId(String userId, Pageable pageable);

    Optional<PostDraft> findByUserIdAndForumId(String userId, Long forumId);

    long countByUserId(String userId);
}

