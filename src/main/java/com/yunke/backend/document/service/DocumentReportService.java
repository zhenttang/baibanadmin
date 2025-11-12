package com.yunke.backend.document.service;

import com.yunke.backend.document.domain.entity.DocumentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DocumentReportService {

    DocumentReport createReport(String documentId, String reporterId, String reporterName,
                               String reason, String description);

    Page<DocumentReport> getReportsByDocument(String documentId, Pageable pageable);

    Page<DocumentReport> getReportsByReporter(String reporterId, Pageable pageable);

    Page<DocumentReport> getPendingReports(Pageable pageable);

    DocumentReport getReportById(Long id);

    DocumentReport reviewReport(Long id, String reviewerId, String reviewerName,
                               String result, String note);

    long getPendingReportCount();

    boolean hasUserReported(String documentId, String reporterId);
}
