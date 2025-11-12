package com.yunke.backend.system.repository;

import com.yunke.backend.system.domain.entity.EntityLike;
import com.yunke.backend.system.domain.entity.EntityLike.EntityType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntityLikeRepository extends JpaRepository<EntityLike, Long> {

    boolean existsByUserIdAndEntityTypeAndEntityId(String userId, EntityType entityType, String entityId);

    Optional<EntityLike> findByUserIdAndEntityTypeAndEntityId(String userId, EntityType entityType, String entityId);

    long countByEntityTypeAndEntityId(EntityType entityType, String entityId);

    void deleteByUserIdAndEntityTypeAndEntityId(String userId, EntityType entityType, String entityId);
}

