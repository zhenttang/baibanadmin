package com.yunke.backend.document.repository;

import com.yunke.backend.document.domain.entity.DocumentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface DocumentReportRepository extends JpaRepository<DocumentReport, Long> {

    Page<DocumentReport> findByDocumentId(String documentId, Pageable pageable);

    Page<DocumentReport> findByReporterId(String reporterId, Pageable pageable);

    Page<DocumentReport> findByStatus(String status, Pageable pageable);

    boolean existsByDocumentIdAndReporterId(String documentId, String reporterId);

    @Query("SELECT COUNT(dr) FROM DocumentReport dr WHERE dr.documentId = :documentId AND dr.status = 'pending'")
    Long countPendingReportsByDocumentId(@Param("documentId") String documentId);

    @Query("SELECT COUNT(dr) FROM DocumentReport dr WHERE dr.status = 'pending'")
    Long countPendingReports();

    @Modifying
    @Transactional
    @Query("UPDATE DocumentReport dr SET dr.status = :status, dr.reviewerId = :reviewerId, " +
           "dr.reviewerName = :reviewerName, dr.reviewResult = :result, dr.reviewNote = :note, " +
           "dr.reviewedAt = :reviewedAt WHERE dr.id = :id")
    int reviewReport(@Param("id") Long id, @Param("status") String status,
                    @Param("reviewerId") String reviewerId, @Param("reviewerName") String reviewerName,
                    @Param("result") String result, @Param("note") String note,
                    @Param("reviewedAt") LocalDateTime reviewedAt);

    List<DocumentReport> findTop10ByStatusOrderByCreatedAtDesc(String status);

    @Query("SELECT dr.documentId, COUNT(dr) as reportCount FROM DocumentReport dr " +
           "WHERE dr.status = 'pending' GROUP BY dr.documentId " +
           "HAVING COUNT(dr) >= :threshold ORDER BY reportCount DESC")
    List<Object[]> findDocumentsWithManyReports(@Param("threshold") int threshold);
}
