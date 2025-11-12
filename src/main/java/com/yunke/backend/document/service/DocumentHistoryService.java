package com.yunke.backend.document.service;

import com.yunke.backend.document.dto.DocHistoryDto;
import com.yunke.backend.common.dto.PaginatedResponse;
import com.yunke.backend.system.domain.entity.SnapshotHistory;
import com.yunke.backend.system.repository.SnapshotHistoryRepository;
import com.yunke.backend.document.service.impl.DocumentHistoryServiceImpl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 文档历史记录服务接口
 */
public interface DocumentHistoryService {

    /**
     * 获取文档历史记录列表
     *
     * @param workspaceId 工作空间ID
     * @param pageDocId   文档ID
     * @param before      时间戳过滤（可选）
     * @param take        获取数量
     * @return 分页的历史记录列表
     */
    PaginatedResponse<DocHistoryDto> getDocumentHistories(
            String workspaceId,
            String pageDocId,
            String before,
            int take
    );

    /**
     * 获取特定时间戳的文档快照
     *
     * @param workspaceId 工作空间ID
     * @param pageDocId   文档ID
     * @param timestamp   时间戳
     * @return 快照数据
     */
    byte[] getDocumentSnapshot(String workspaceId, String pageDocId, String timestamp);

    /**
     * 恢复文档到指定版本
     *
     * @param workspaceId 工作空间ID
     * @param docId       文档ID
     * @param timestamp   时间戳
     */
    void recoverDocumentVersion(String workspaceId, String docId, String timestamp);

    /**
     * 保存文档快照
     *
     * @param workspaceId 工作空间ID
     * @param docId       文档ID
     * @param blob        快照数据
     * @param state       状态数据
     * @param createdBy   创建者
     * @return 保存的快照历史记录
     */
    SnapshotHistory saveDocumentSnapshot(
            String workspaceId,
            String docId,
            byte[] blob,
            byte[] state,
            String createdBy
    );

    /**
     * 获取文档的最新快照
     *
     * @param workspaceId 工作空间ID
     * @param docId       文档ID
     * @return 最新的快照历史记录
     */
    Optional<SnapshotHistory> getLatestSnapshot(String workspaceId, String docId);

    /**
     * 清理过期的历史记录
     *
     * @return 清理的记录数
     */
    int cleanupExpiredHistories();

    /**
     * 将 SnapshotHistory 实体转换为 DocHistoryDto
     *
     * @param snapshotHistory 快照历史记录实体
     * @return 文档历史记录 DTO
     */
    DocHistoryDto convertToDto(SnapshotHistory snapshotHistory);
}