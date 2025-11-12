package com.yunke.backend.workspace.service;

import com.yunke.backend.workspace.domain.entity.WorkspaceDoc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

/**
 * 工作空间文档服务接口
 */
public interface WorkspaceDocService {

    /**
     * 创建文档
     */
    WorkspaceDoc createDoc(String workspaceId, String userId, String title, String docId);

    /**
     * 根据ID查找文档
     */
    Optional<WorkspaceDoc> findById(String id);
    
    /**
     * 根据文档ID查找工作空间ID
     */
    Optional<String> findWorkspaceIdByDocId(String docId);

    /**
     * 更新文档
     */
    WorkspaceDoc updateDoc(WorkspaceDoc doc);

    /**
     * 删除文档
     */
    void deleteDoc(String id);

    /**
     * 获取工作空间的文档列表
     */
    List<WorkspaceDoc> getWorkspaceDocs(String workspaceId);

    /**
     * 分页获取工作空间文档
     */
    Page<WorkspaceDoc> getWorkspaceDocs(String workspaceId, Pageable pageable);

    /**
     * 搜索文档
     */
    List<WorkspaceDoc> searchDocs(String workspaceId, String keyword);

    /**
     * 获取用户最近访问的文档
     */
    List<WorkspaceDoc> getRecentDocs(String userId, int limit);

    /**
     * 记录文档访问
     */
    void recordDocAccess(String docId, String userId);

    /**
     * 获取文档协作者列表
     */
    List<String> getDocCollaborators(String docId);

    /**
     * 检查用户是否有文档访问权限
     */
    boolean hasDocAccess(String docId, String userId);

    /**
     * 检查用户是否有文档编辑权限
     */
    boolean hasDocEditPermission(String docId, String userId);

    /**
     * 设置文档标题
     */
    void setDocTitle(String docId, String title);

    /**
     * 设置文档公开状态
     */
    void setDocPublic(String docId, boolean isPublic, String publicPermission, String publicMode);

    /**
     * 获取文档统计信息
     */
    DocStats getDocStats(String docId);

    /**
     * 应用YJS更新数据到文档
     */
    boolean applyYjsUpdate(String workspaceId, String docId, byte[] updateData, String userId, Long timestamp);

    /**
     * 应用YJS更新数据到文档 (Socket.IO专用，支持Base64编码数据)
     */
    long applyYjsUpdate(String workspaceId, String docId, String base64UpdateData);

    /**
     * 获取文档最后更新时间戳
     */
    long getDocTimestamp(String workspaceId, String docId);

    /**
     * 文档统计信息
     */
    record DocStats(
            int viewCount,
            int editCount,
            int collaboratorCount,
            java.time.Instant lastModified
    ) {}
}
