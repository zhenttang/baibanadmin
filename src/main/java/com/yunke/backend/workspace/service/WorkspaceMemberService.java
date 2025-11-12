package com.yunke.backend.workspace.service;

import com.yunke.backend.workspace.domain.entity.WorkspaceMember;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 工作空间成员服务接口 - 完全按照AFFiNE实现
 * 对应: AFFiNE中的工作空间成员管理功能
 */
public interface WorkspaceMemberService {

    /**
     * 获取用户的活跃角色
     * 对应: QuotaService中getUserStorageUsage使用的方法
     */
    Mono<List<WorkspaceMember>> getUserActiveRoles(String userId, String role);

    /**
     * 获取工作空间的所有者
     * 对应: 配额继承逻辑中需要的方法
     */
    Mono<WorkspaceMember> getOwner(String workspaceId);

    /**
     * 获取工作空间的收费成员数量
     * 对应: QuotaService中getWorkspaceQuotaWithUsage使用的方法
     */
    Mono<Integer> getChargedCount(String workspaceId);

    /**
     * 检查用户是否为工作空间所有者
     */
    Mono<Boolean> isOwner(String workspaceId, String userId);

    /**
     * 获取工作空间的所有成员
     */
    Mono<List<WorkspaceMember>> getMembers(String workspaceId);

    /**
     * 获取工作空间成员数量
     */
    Mono<Integer> getMemberCount(String workspaceId);
}