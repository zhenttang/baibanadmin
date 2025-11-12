package com.yunke.backend.document.service.impl;

import com.yunke.backend.document.domain.entity.DocumentReport;

import com.yunke.backend.document.repository.DocumentReportRepository;
import com.yunke.backend.document.service.DocumentReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentReportServiceImpl implements DocumentReportService {

    private final DocumentReportRepository reportRepository;

    @Override
    @Transactional
    public DocumentReport createReport(String documentId, String reporterId, String reporterName,
                                      String reason, String description) {
        if (reportRepository.existsByDocumentIdAndReporterId(documentId, reporterId)) {
            throw new RuntimeException("您已经举报过该文档");
        }

        DocumentReport report = new DocumentReport();
        report.setDocumentId(documentId);
        report.setReporterId(reporterId);
        report.setReporterName(reporterName);
        report.setReason(reason);
        report.setDescription(description);
        report.setStatus("pending");

        DocumentReport saved = reportRepository.save(report);
        log.info("Created document report {} for document {} by user {}", saved.getId(), documentId, reporterId);

        return saved;
    }

    @Override
    public Page<DocumentReport> getReportsByDocument(String documentId, Pageable pageable) {
        return reportRepository.findByDocumentId(documentId, pageable);
    }

    @Override
    public Page<DocumentReport> getReportsByReporter(String reporterId, Pageable pageable) {
        return reportRepository.findByReporterId(reporterId, pageable);
    }

    @Override
    public Page<DocumentReport> getPendingReports(Pageable pageable) {
        return reportRepository.findByStatus("pending", pageable);
    }

    @Override
    public DocumentReport getReportById(Long id) {
        return reportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("举报记录不存在"));
    }

    @Override
    @Transactional
    public DocumentReport reviewReport(Long id, String reviewerId, String reviewerName,
                                      String result, String note) {
        DocumentReport report = getReportById(id);

        if (!"pending".equals(report.getStatus())) {
            throw new RuntimeException("该举报已经被处理过了");
        }

        String newStatus = "approved".equals(result) ? "approved" : "rejected";

        reportRepository.reviewReport(
            id, newStatus, reviewerId, reviewerName, result, note, LocalDateTime.now()
        );

        log.info("Reviewed report {} with result {} by reviewer {}", id, result, reviewerId);

        return getReportById(id);
    }

    @Override
    public long getPendingReportCount() {
        return reportRepository.countPendingReports();
    }

    @Override
    public boolean hasUserReported(String documentId, String reporterId) {
        return reportRepository.existsByDocumentIdAndReporterId(documentId, reporterId);
    }
}
