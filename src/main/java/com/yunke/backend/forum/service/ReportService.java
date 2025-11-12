package com.yunke.backend.forum.service;

import com.yunke.backend.forum.dto.CreateReportRequest;
import com.yunke.backend.forum.dto.HandleReportRequest;
import com.yunke.backend.forum.dto.ReportDTO;
import com.yunke.backend.forum.domain.entity.ForumReport;
import com.yunke.backend.forum.repository.ForumReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportService {

    private final ForumReportRepository reportRepository;

    // 1. 创建举报
    @Transactional(rollbackFor = Exception.class)
    public ReportDTO createReport(CreateReportRequest request, Long reporterId) {
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (reporterId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }

        ForumReport.TargetType targetType;
        ForumReport.ReportReason reason;
        try {
            targetType = ForumReport.TargetType.valueOf(request.getTargetType().trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的目标类型: " + request.getTargetType());
        }
        try {
            reason = ForumReport.ReportReason.valueOf(request.getReason().trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的举报原因: " + request.getReason());
        }

        ForumReport entity = new ForumReport();
        entity.setTargetType(targetType);
        entity.setTargetId(request.getTargetId());
        entity.setReporterId(String.valueOf(reporterId));
        entity.setReason(reason);
        entity.setDescription(request.getDescription());
        entity.setStatus(ForumReport.ReportStatus.PENDING);

        ForumReport saved = reportRepository.save(entity);
        return toDTO(saved);
    }

    // 2. 获取待处理举报列表（按创建时间倒序）
    @Transactional(readOnly = true)
    public List<ReportDTO> getPendingReports() {
        List<ForumReport> list = reportRepository.findByStatus(
                ForumReport.ReportStatus.PENDING,
                Sort.by("createdAt").descending()
        );
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    // 3. 获取我的举报历史（按创建时间倒序）
    @Transactional(readOnly = true)
    public List<ReportDTO> getMyReports(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        List<ForumReport> list = reportRepository.findByReporterIdOrderByCreatedAtDesc(String.valueOf(userId));
        return list.stream().map(this::toDTO).collect(Collectors.toList());
    }

    // 4. 处理举报（设置状态、处理人、备注、时间）
    @Transactional(rollbackFor = Exception.class)
    public ReportDTO handleReport(Long reportId, HandleReportRequest request, Long handlerId) {
        if (reportId == null) {
            throw new IllegalArgumentException("举报ID不能为空");
        }
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (handlerId == null) {
            throw new IllegalArgumentException("处理人ID不能为空");
        }

        ForumReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("举报记录不存在"));

        ForumReport.ReportStatus newStatus;
        try {
            newStatus = ForumReport.ReportStatus.valueOf(request.getStatus().trim().toUpperCase(Locale.ROOT));
        } catch (Exception e) {
            throw new IllegalArgumentException("无效的举报状态: " + request.getStatus());
        }
        if (newStatus == ForumReport.ReportStatus.PENDING) {
            throw new IllegalArgumentException("处理状态不能为PENDING");
        }

        report.setStatus(newStatus);
        report.setHandlerId(String.valueOf(handlerId));
        report.setHandlerNote(request.getHandleNote());
        report.setHandledAt(LocalDateTime.now());

        ForumReport saved = reportRepository.save(report);
        return toDTO(saved);
    }

    // 5. 实体转DTO
    public ReportDTO toDTO(ForumReport entity) {
        if (entity == null) return null;
        ReportDTO dto = new ReportDTO();
        dto.setId(entity.getId());
        dto.setTargetType(entity.getTargetType() == null ? null : entity.getTargetType().name());
        dto.setTargetId(entity.getTargetId());
        // reporterId/handlerId 在实体中是字符串，这里尝试转换为Long
        try {
            dto.setReporterId(entity.getReporterId() == null ? null : Long.parseLong(entity.getReporterId()));
        } catch (NumberFormatException ignored) {
            dto.setReporterId(null);
        }
        dto.setReporterName(entity.getReporterName());
        dto.setReason(entity.getReason() == null ? null : entity.getReason().name());
        dto.setDescription(entity.getDescription());
        dto.setStatus(entity.getStatus() == null ? null : entity.getStatus().name());
        try {
            dto.setHandlerId(entity.getHandlerId() == null ? null : Long.parseLong(entity.getHandlerId()));
        } catch (NumberFormatException ignored) {
            dto.setHandlerId(null);
        }
        dto.setHandlerName(entity.getHandlerName());
        dto.setHandleNote(entity.getHandlerNote());
        dto.setHandledAt(entity.getHandledAt());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}

