package com.yunke.backend.workspace.service.impl;

import com.yunke.backend.workspace.domain.entity.WorkspaceMember;
import com.yunke.backend.workspace.repository.WorkspaceMemberRepository;
import com.yunke.backend.workspace.service.WorkspaceMemberService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

/**
 * 工作空间成员服务实现 - 完全按照AFFiNE实现
 * 对应: AFFiNE中的工作空间成员管理功能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceMemberServiceImpl implements WorkspaceMemberService {

    private final WorkspaceMemberRepository workspaceMemberRepository;

    @Override
    public Mono<List<WorkspaceMember>> getUserActiveRoles(String userId, String role) {
        return Mono.fromCallable(() -> {
            log.debug("获取用户活跃角色: userId={}, role={}", userId, role);
            
            if (role != null) {
                return workspaceMemberRepository.findByUserIdAndRole(userId, role);
            } else {
                return workspaceMemberRepository.findByUserId(userId);
            }
        });
    }

    @Override
    public Mono<WorkspaceMember> getOwner(String workspaceId) {
        return Mono.fromCallable(() -> {
            log.debug("获取工作空间所有者: workspaceId={}", workspaceId);
            
            Optional<WorkspaceMember> owner = workspaceMemberRepository.findByWorkspaceIdAndRole(workspaceId, "Owner");
            return owner.orElse(null);
        });
    }

    @Override
    public Mono<Integer> getChargedCount(String workspaceId) {
        return Mono.fromCallable(() -> {
            log.debug("获取工作空间收费成员数量: workspaceId={}", workspaceId);
            
            // 计算收费成员数量（通常除免费用户外的所有活跃成员）
            List<WorkspaceMember> members = workspaceMemberRepository.findByWorkspaceId(workspaceId);
            return (int) members.stream()
                .filter(member -> member.isActive())
                .filter(member -> !"Guest".equals(member.getRole()))
                .count();
        });
    }

    @Override
    public Mono<Boolean> isOwner(String workspaceId, String userId) {
        return Mono.fromCallable(() -> {
            log.debug("检查用户是否为工作空间所有者: workspaceId={}, userId={}", workspaceId, userId);
            
            Optional<WorkspaceMember> member = workspaceMemberRepository.findByWorkspaceIdAndUserId(workspaceId, userId);
            return member.map(m -> "Owner".equals(m.getRole())).orElse(false);
        });
    }

    @Override
    public Mono<List<WorkspaceMember>> getMembers(String workspaceId) {
        return Mono.fromCallable(() -> {
            log.debug("获取工作空间所有成员: workspaceId={}", workspaceId);
            
            return workspaceMemberRepository.findByWorkspaceId(workspaceId);
        });
    }

    @Override
    public Mono<Integer> getMemberCount(String workspaceId) {
        return Mono.fromCallable(() -> {
            log.debug("获取工作空间成员数量: workspaceId={}", workspaceId);
            
            return workspaceMemberRepository.countByWorkspaceId(workspaceId);
        });
    }
}