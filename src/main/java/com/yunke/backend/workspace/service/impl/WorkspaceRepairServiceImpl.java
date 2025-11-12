package com.yunke.backend.workspace.service.impl;

import com.yunke.backend.workspace.domain.entity.Workspace;

import com.yunke.backend.workspace.domain.entity.WorkspaceUserRole;
import com.yunke.backend.workspace.repository.WorkspaceRepository;
import com.yunke.backend.system.repository.SnapshotRepository;

import com.yunke.backend.document.service.RootDocumentService;
import com.yunke.backend.workspace.repository.WorkspaceUserRoleRepository;
import com.yunke.backend.workspace.service.WorkspaceRepairService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * 工作空间修复服务实现
 * 用于修复现有工作空间的问题，如批量创建缺失的根文档
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceRepairServiceImpl implements WorkspaceRepairService {

    private final WorkspaceRepository workspaceRepository;
    private final SnapshotRepository snapshotRepository;
    private final WorkspaceUserRoleRepository workspaceUserRoleRepository;
    private final RootDocumentService rootDocumentService;

    @Override
    public Mono<RootDocumentCheckResult> checkAllWorkspacesRootDocuments() {
        log.info("🔍 [REPAIR-SERVICE] 开始检查所有工作空间的根文档状态");

        return Mono.fromCallable(() -> {
            List<Workspace> allWorkspaces = workspaceRepository.findAll();
            int totalWorkspaces = allWorkspaces.size();
            int workspacesWithRootDoc = 0;
            int workspacesWithoutRootDoc = 0;

            for (Workspace workspace : allWorkspaces) {
                boolean hasRootDoc = snapshotRepository.existsByWorkspaceIdAndId(
                        workspace.getId(), workspace.getId());

                if (hasRootDoc) {
                    workspacesWithRootDoc++;
                    log.debug("✅ [REPAIR-CHECK] 工作空间有根文档: id='{}', name='{}'",
                            workspace.getId(), workspace.getName());
                } else {
                    workspacesWithoutRootDoc++;
                    log.warn("❌ [REPAIR-CHECK] 工作空间缺少根文档: id='{}', name='{}'",
                            workspace.getId(), workspace.getName());
                }
            }

            RootDocumentCheckResult result = new RootDocumentCheckResult(
                    totalWorkspaces, workspacesWithRootDoc, workspacesWithoutRootDoc);
            
            log.info("📊 [REPAIR-CHECK] 检查完成: 总数={}, 有根文档={}, 缺少根文档={}",
                    totalWorkspaces, workspacesWithRootDoc, workspacesWithoutRootDoc);
                    
            return result;
        });
    }

    @Override
    @Transactional
    public Mono<RootDocumentRepairResult> repairAllMissingRootDocuments() {
        log.info("🛠️ [REPAIR-SERVICE] 开始批量修复缺失的根文档");

        return Mono.fromCallable(() -> workspaceRepository.findAll())
                .flatMapMany(Flux::fromIterable)
                .flatMap(workspace -> {
                    return Mono.fromCallable(() -> {
                        // 检查是否已有根文档
                        boolean hasRootDoc = snapshotRepository.existsByWorkspaceIdAndId(
                                workspace.getId(), workspace.getId());
                        return new WorkspaceWithRootDocStatus(workspace, hasRootDoc);
                    });
                })
                .filter(status -> !status.hasRootDoc()) // 只处理没有根文档的工作空间
                .flatMap(status -> {
                    String workspaceId = status.workspace().getId();
                    
                    // 找到工作空间的所有者作为根文档创建者
                    return Mono.fromCallable(() -> {
                        List<WorkspaceUserRole> owners = workspaceUserRoleRepository
                                .findByWorkspaceIdAndType(workspaceId, WorkspaceUserRole.WorkspaceRole.OWNER);
                        
                        String creatorUserId = "system"; // 默认使用系统用户
                        if (!owners.isEmpty()) {
                            creatorUserId = owners.get(0).getUserId();
                        }
                        
                        log.info("🔧 [REPAIR-SERVICE] 为工作空间创建根文档: workspaceId='{}', creator='{}'",
                                workspaceId, creatorUserId);
                        
                        return new WorkspaceRepairTask(status.workspace(), creatorUserId);
                    });
                })
                .flatMap(task -> {
                    // 执行根文档创建
                    return rootDocumentService.createRootDocument(
                            task.workspace().getId(), task.creatorUserId())
                            .map(success -> new RepairResult(task.workspace(), success, null))
                            .onErrorResume(error -> {
                                log.error("❌ [REPAIR-SERVICE] 创建根文档失败: workspaceId='{}', error={}",
                                        task.workspace().getId(), error.getMessage(), error);
                                return Mono.just(new RepairResult(task.workspace(), false, error));
                            });
                })
                .collectList()
                .map(results -> {
                    int totalProcessed = results.size();
                    int successCount = 0;
                    int errorCount = 0;
                    
                    for (RepairResult result : results) {
                        if (result.success()) {
                            successCount++;
                            log.info("✅ [REPAIR-SERVICE] 根文档创建成功: workspaceId='{}', name='{}'",
                                    result.workspace().getId(), result.workspace().getName());
                        } else {
                            errorCount++;
                        }
                    }
                    
                    int skippedCount = 0; // 在此流程中，跳过的工作空间已被过滤掉
                    
                    RootDocumentRepairResult repairResult = new RootDocumentRepairResult(
                            totalProcessed, successCount, skippedCount, errorCount);
                    
                    log.info("🏁 [REPAIR-SERVICE] 批量修复完成: 处理={}, 成功={}, 跳过={}, 错误={}",
                            totalProcessed, successCount, skippedCount, errorCount);
                            
                    return repairResult;
                });
    }

    @Override
    public Mono<Boolean> repairWorkspaceRootDocument(String workspaceId) {
        log.info("🎯 [REPAIR-SERVICE] 修复特定工作空间的根文档: workspaceId='{}'", workspaceId);

        return Mono.fromCallable(() -> {
            // 检查工作空间是否存在
            return workspaceRepository.findById(workspaceId).orElse(null);
        })
        .flatMap(workspace -> {
            if (workspace == null) {
                log.warn("⚠️ [REPAIR-SERVICE] 工作空间不存在: workspaceId='{}'", workspaceId);
                return Mono.just(false);
            }
            
            // 找到工作空间的所有者
            return Mono.fromCallable(() -> {
                List<WorkspaceUserRole> owners = workspaceUserRoleRepository
                        .findByWorkspaceIdAndType(workspaceId, WorkspaceUserRole.WorkspaceRole.OWNER);
                        
                String creatorUserId = "system";
                if (!owners.isEmpty()) {
                    creatorUserId = owners.get(0).getUserId();
                }
                
                return creatorUserId;
            })
            .flatMap(creatorUserId -> {
                return rootDocumentService.createRootDocument(workspaceId, creatorUserId)
                        .doOnSuccess(success -> {
                            if (success) {
                                log.info("✅ [REPAIR-SERVICE] 根文档创建成功: workspaceId='{}'", workspaceId);
                            } else {
                                log.warn("⚠️ [REPAIR-SERVICE] 根文档创建跳过（已存在）: workspaceId='{}'", workspaceId);
                            }
                        })
                        .doOnError(error -> {
                            log.error("❌ [REPAIR-SERVICE] 根文档创建失败: workspaceId='{}', error={}",
                                    workspaceId, error.getMessage(), error);
                        });
            });
        });
    }

    // 内部辅助记录类
    private record WorkspaceWithRootDocStatus(Workspace workspace, boolean hasRootDoc) {}
    private record WorkspaceRepairTask(Workspace workspace, String creatorUserId) {}
    private record RepairResult(Workspace workspace, boolean success, Throwable error) {}
}