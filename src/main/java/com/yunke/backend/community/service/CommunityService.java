package com.yunke.backend.community.service;

import com.yunke.backend.community.dto.CommunityDocDto;
import com.yunke.backend.community.enums.CommunityPermission;
import reactor.core.publisher.Mono;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * 社区服务接口
 */
public interface CommunityService {
    
    /**
     * 分享文档到社区
     * @param docId 文档ID
     * @param workspaceId 工作空间ID
     * @param userId 用户ID
     * @param permission 社区权限
     * @param title 社区标题
     * @param description 社区描述
     * @return 操作结果
     */
    Mono<Boolean> shareDocToCommunity(String docId, String workspaceId, String userId, 
                                     CommunityPermission permission, String title, String description);
    
    /**
     * 获取社区文档列表（根据用户权限过滤）
     * @param workspaceId 工作空间ID
     * @param userId 用户ID
     * @param page 页码
     * @param size 页大小
     * @param search 搜索关键词
     * @return 社区文档列表
     */
    Mono<Page<CommunityDocDto>> getCommunityDocs(String workspaceId, String userId, 
                                                int page, int size, String search);
    
    /**
     * 取消文档在社区的分享
     * @param docId 文档ID
     * @param workspaceId 工作空间ID
     * @param userId 用户ID
     * @return 操作结果
     */
    Mono<Boolean> unshareDocFromCommunity(String docId, String workspaceId, String userId);
    
    /**
     * 更新文档社区权限
     * @param docId 文档ID
     * @param workspaceId 工作空间ID
     * @param userId 用户ID
     * @param permission 新权限
     * @return 操作结果
     */
    Mono<Boolean> updateCommunityPermission(String docId, String workspaceId, String userId, 
                                           CommunityPermission permission);
    
    /**
     * 增加文档浏览次数
     * @param docId 文档ID
     * @param workspaceId 工作空间ID
     * @return 操作结果
     */
    Mono<Boolean> incrementViewCount(String docId, String workspaceId);
    
    /**
     * 检查用户是否可以查看社区文档
     * @param docId 文档ID
     * @param workspaceId 工作空间ID
     * @param userId 用户ID
     * @return 是否可以查看
     */
    Mono<Boolean> canUserViewCommunityDoc(String docId, String workspaceId, String userId);
    
    /**
     * 获取用户可见的权限级别
     * @param workspaceRole 工作空间角色
     * @return 可见权限列表
     */
    List<CommunityPermission> getUserVisiblePermissions(String workspaceRole);
}