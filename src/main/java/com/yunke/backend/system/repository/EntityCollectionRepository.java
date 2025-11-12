package com.yunke.backend.system.repository;

import com.yunke.backend.system.domain.entity.EntityCollection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntityCollectionRepository extends JpaRepository<EntityCollection, Long> {

    boolean existsByUserIdAndEntityTypeAndEntityId(String userId, String entityType, String entityId);

    Optional<EntityCollection> findByUserIdAndEntityTypeAndEntityId(String userId, String entityType, String entityId);

    void deleteByUserIdAndEntityTypeAndEntityId(String userId, String entityType, String entityId);

    Page<EntityCollection> findByUserIdAndEntityTypeOrderByCreatedAtDesc(String userId, String entityType, Pageable pageable);
}

