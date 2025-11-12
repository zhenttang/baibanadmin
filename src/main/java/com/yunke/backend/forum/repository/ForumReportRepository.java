package com.yunke.backend.forum.repository;

import com.yunke.backend.forum.domain.entity.ForumReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ForumReportRepository extends JpaRepository<ForumReport, Long> {
    
    Page<ForumReport> findByStatusOrderByCreatedAtDesc(
        ForumReport.ReportStatus status, 
        Pageable pageable
    );
    
    Page<ForumReport> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(
        ForumReport.TargetType targetType, 
        String targetId, 
        Pageable pageable
    );
    
    boolean existsByReporterIdAndTargetTypeAndTargetId(
        String reporterId, 
        ForumReport.TargetType targetType, 
        String targetId
    );

    // 补充：按状态查询（支持外部排序）
    java.util.List<ForumReport> findByStatus(ForumReport.ReportStatus status, Sort sort);

    // 补充：查询某用户的举报历史（按创建时间倒序）
    java.util.List<ForumReport> findByReporterIdOrderByCreatedAtDesc(String reporterId);
}
