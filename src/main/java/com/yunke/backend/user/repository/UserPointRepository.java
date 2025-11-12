package com.yunke.backend.user.repository;

import com.yunke.backend.user.domain.entity.UserPoint;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserPointRepository extends JpaRepository<UserPoint, Long> {
    
    Optional<UserPoint> findByUserId(String userId);
    
    Page<UserPoint> findAllByOrderByTotalPointsDesc(Pageable pageable);
    
    Page<UserPoint> findAllByOrderByReputationDesc(Pageable pageable);
}
