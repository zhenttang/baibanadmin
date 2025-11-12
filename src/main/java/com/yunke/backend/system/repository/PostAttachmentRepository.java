package com.yunke.backend.system.repository;

import com.yunke.backend.system.domain.entity.PostAttachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PostAttachmentRepository extends JpaRepository<PostAttachment, Long> {
    List<PostAttachment> findByPostIdOrderByCreatedAtDesc(String postId);

    Page<PostAttachment> findByUploaderId(String uploaderId, Pageable pageable);

    Long countByPostId(String postId);
}

