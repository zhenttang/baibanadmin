package com.yunke.backend.system.controller;

import com.yunke.backend.system.domain.entity.GanttTaskDependency;
import com.yunke.backend.system.domain.entity.GanttViewConfig;
import com.yunke.backend.system.service.GanttManagementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 甘特图管理控制器
 * 提供甘特图配置和依赖关系管理的REST API
 * 
 * @author AFFiNE Development Team
 */
@RestController
@RequestMapping("/api/workspaces/{workspaceId}/gantt")
@RequiredArgsConstructor
@Slf4j
public class GanttController {
    
    private final GanttManagementService ganttManagementService;
    
    // ============ 甘特图配置管理 ============
    
    /**
     * 获取甘特图配置
     */
    @GetMapping("/{docId}/config")
    public Mono<ResponseEntity<Map<String, Object>>> getGanttConfig(
            @PathVariable String workspaceId,
            @PathVariable String docId) {
        
        log.debug("Getting gantt config for workspace: {}, doc: {}", workspaceId, docId);
        
        return ganttManagementService.getGanttConfig(workspaceId, docId)
            .map(config -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", buildConfigResponse(config));
                return ResponseEntity.ok(response);
            })
            .switchIfEmpty(Mono.fromCallable(() -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "Gantt config not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }))
            .onErrorResume(error -> {
                log.error("Failed to get gantt config: {}", error.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", error.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
            });
    }
    
    /**
     * 创建甘特图配置
     */
    @PostMapping("/{docId}/config")
    public Mono<ResponseEntity<Map<String, Object>>> createGanttConfig(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestBody @Valid CreateGanttConfigRequest request) {
        
        String userId = getCurrentUserId();
        log.info("Creating gantt config for workspace: {}, doc: {}, user: {}", 
                workspaceId, docId, userId);
        
        return ganttManagementService.createGanttConfig(
                workspaceId, docId, 
                request.timelineConfig, 
                request.displayConfig, 
                request.workingCalendar, 
                userId
            )
            .map(config -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", buildConfigResponse(config));
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            })
            .onErrorResume(error -> {
                log.error("Failed to create gantt config: {}", error.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", error.getMessage());
                
                HttpStatus status = error instanceof IllegalArgumentException ? 
                    HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
                    
                return Mono.just(ResponseEntity.status(status).body(response));
            });
    }
    
    /**
     * 更新甘特图配置
     */
    @PutMapping("/{docId}/config")
    public Mono<ResponseEntity<Map<String, Object>>> updateGanttConfig(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestBody @Valid UpdateGanttConfigRequest request) {
        
        String userId = getCurrentUserId();
        log.info("Updating gantt config for workspace: {}, doc: {}, user: {}", 
                workspaceId, docId, userId);
        
        return ganttManagementService.updateGanttConfig(
                workspaceId, docId, 
                request.timelineConfig, 
                request.displayConfig, 
                request.workingCalendar, 
                userId
            )
            .map(config -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", buildConfigResponse(config));
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                log.error("Failed to update gantt config: {}", error.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", error.getMessage());
                
                HttpStatus status = error instanceof IllegalArgumentException ? 
                    HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
                    
                return Mono.just(ResponseEntity.status(status).body(response));
            });
    }
    
    /**
     * 删除甘特图配置
     */
    @DeleteMapping("/{docId}/config")
    public Mono<ResponseEntity<Map<String, Object>>> deleteGanttConfig(
            @PathVariable String workspaceId,
            @PathVariable String docId) {
        
        String userId = getCurrentUserId();
        log.info("Deleting gantt config for workspace: {}, doc: {}, user: {}", 
                workspaceId, docId, userId);
        
        return ganttManagementService.deleteGanttConfig(workspaceId, docId, userId)
            .then(Mono.fromCallable(() -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Gantt config deleted successfully");
                return ResponseEntity.ok(response);
            }))
            .onErrorResume(error -> {
                log.error("Failed to delete gantt config: {}", error.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", error.getMessage());
                
                HttpStatus status = error instanceof IllegalArgumentException ? 
                    HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
                    
                return Mono.just(ResponseEntity.status(status).body(response));
            });
    }
    
    // ============ 任务依赖关系管理 ============
    
    /**
     * 获取任务依赖关系
     */
    @GetMapping("/{docId}/dependencies")
    public Mono<ResponseEntity<Map<String, Object>>> getTaskDependencies(
            @PathVariable String workspaceId,
            @PathVariable String docId) {
        
        log.debug("Getting task dependencies for workspace: {}, doc: {}", workspaceId, docId);
        
        return ganttManagementService.getTaskDependencies(workspaceId, docId)
            .map(this::buildDependencyResponse)
            .collectList()
            .map(dependencies -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", dependencies);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                log.error("Failed to get task dependencies: {}", error.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", error.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
            });
    }
    
    /**
     * 创建任务依赖关系
     */
    @PostMapping("/{docId}/dependencies")
    public Mono<ResponseEntity<Map<String, Object>>> createTaskDependency(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestBody @Valid CreateDependencyRequest request) {
        
        String userId = getCurrentUserId();
        log.info("Creating task dependency: {} -> {}, workspace: {}, doc: {}, user: {}", 
                request.fromTaskId, request.toTaskId, workspaceId, docId, userId);
        
        return ganttManagementService.createTaskDependency(
                workspaceId, docId, 
                request.fromTaskId, 
                request.toTaskId, 
                GanttTaskDependency.DependencyType.fromValue(request.dependencyType),
                request.lagDays, 
                userId
            )
            .map(dependency -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", buildDependencyResponse(dependency));
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            })
            .onErrorResume(error -> {
                log.error("Failed to create task dependency: {}", error.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", error.getMessage());
                
                HttpStatus status = error instanceof IllegalArgumentException ? 
                    HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
                    
                return Mono.just(ResponseEntity.status(status).body(response));
            });
    }
    
    /**
     * 批量创建任务依赖关系
     */
    @PostMapping("/{docId}/dependencies/batch")
    public Mono<ResponseEntity<Map<String, Object>>> createTaskDependencies(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestBody @Valid BatchCreateDependenciesRequest request) {
        
        String userId = getCurrentUserId();
        log.info("Creating {} task dependencies for workspace: {}, doc: {}, user: {}", 
                request.dependencies.size(), workspaceId, docId, userId);
        
        List<GanttManagementService.CreateDependencyRequest> serviceRequests = 
            request.dependencies.stream()
                .map(req -> new GanttManagementService.CreateDependencyRequest(
                    req.fromTaskId,
                    req.toTaskId,
                    GanttTaskDependency.DependencyType.fromValue(req.dependencyType),
                    req.lagDays,
                    req.isFlexible
                ))
                .toList();
        
        return ganttManagementService.createTaskDependencies(workspaceId, docId, serviceRequests, userId)
            .map(this::buildDependencyResponse)
            .collectList()
            .map(dependencies -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", dependencies);
                response.put("created", dependencies.size());
                return ResponseEntity.status(HttpStatus.CREATED).body(response);
            })
            .onErrorResume(error -> {
                log.error("Failed to create task dependencies: {}", error.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", error.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
            });
    }
    
    /**
     * 删除任务依赖关系
     */
    @DeleteMapping("/{docId}/dependencies/{dependencyId}")
    public Mono<ResponseEntity<Map<String, Object>>> deleteTaskDependency(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @PathVariable Long dependencyId) {
        
        String userId = getCurrentUserId();
        log.info("Deleting task dependency: {}, workspace: {}, doc: {}, user: {}", 
                dependencyId, workspaceId, docId, userId);
        
        return ganttManagementService.deleteTaskDependency(dependencyId, userId)
            .then(Mono.fromCallable(() -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "Task dependency deleted successfully");
                return ResponseEntity.ok(response);
            }))
            .onErrorResume(error -> {
                log.error("Failed to delete task dependency: {}", error.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", error.getMessage());
                
                HttpStatus status = error instanceof IllegalArgumentException ? 
                    HttpStatus.BAD_REQUEST : HttpStatus.INTERNAL_SERVER_ERROR;
                    
                return Mono.just(ResponseEntity.status(status).body(response));
            });
    }
    
    /**
     * 获取任务的前置依赖
     */
    @GetMapping("/{docId}/tasks/{taskId}/predecessors")
    public Mono<ResponseEntity<Map<String, Object>>> getTaskPredecessors(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @PathVariable String taskId) {
        
        return ganttManagementService.getTaskPredecessors(workspaceId, docId, taskId)
            .map(this::buildDependencyResponse)
            .collectList()
            .map(dependencies -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", dependencies);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                log.error("Failed to get task predecessors: {}", error.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", error.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
            });
    }
    
    /**
     * 获取任务的后续依赖
     */
    @GetMapping("/{docId}/tasks/{taskId}/successors")
    public Mono<ResponseEntity<Map<String, Object>>> getTaskSuccessors(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @PathVariable String taskId) {
        
        return ganttManagementService.getTaskSuccessors(workspaceId, docId, taskId)
            .map(this::buildDependencyResponse)
            .collectList()
            .map(dependencies -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", dependencies);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                log.error("Failed to get task successors: {}", error.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", error.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
            });
    }
    
    /**
     * 检查循环依赖
     */
    @GetMapping("/{docId}/dependencies/check-circular")
    public Mono<ResponseEntity<Map<String, Object>>> checkCircularDependency(
            @PathVariable String workspaceId,
            @PathVariable String docId,
            @RequestParam(required = false) String fromTaskId,
            @RequestParam(required = false) String toTaskId) {
        
        Mono<Boolean> checkMono;
        
        if (fromTaskId != null && toTaskId != null) {
            // 检查添加特定依赖是否会产生循环
            checkMono = ganttManagementService.wouldCreateCircularDependency(
                workspaceId, docId, fromTaskId, toTaskId
            );
        } else {
            // 检查现有依赖是否存在循环
            checkMono = ganttManagementService.hasCircularDependency(workspaceId, docId);
        }
        
        return checkMono
            .map(hasCircular -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("hasCircularDependency", hasCircular);
                
                if (fromTaskId != null && toTaskId != null) {
                    response.put("wouldCreateCircular", hasCircular);
                    response.put("fromTaskId", fromTaskId);
                    response.put("toTaskId", toTaskId);
                }
                
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                log.error("Failed to check circular dependency: {}", error.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", error.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
            });
    }
    
    // ============ 统计和分析 ============
    
    /**
     * 获取甘特图统计信息
     */
    @GetMapping("/{docId}/statistics")
    public Mono<ResponseEntity<Map<String, Object>>> getGanttStatistics(
            @PathVariable String workspaceId,
            @PathVariable String docId) {
        
        return ganttManagementService.getGanttStatistics(workspaceId, docId)
            .map(statistics -> {
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("data", statistics);
                return ResponseEntity.ok(response);
            })
            .onErrorResume(error -> {
                log.error("Failed to get gantt statistics: {}", error.getMessage());
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("error", error.getMessage());
                return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response));
            });
    }
    
    // ============ 私有辅助方法 ============
    
    private String getCurrentUserId() {
        return SecurityContextHolder.getContext().getAuthentication().getName();
    }
    
    private Map<String, Object> buildConfigResponse(GanttViewConfig config) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", config.getId());
        response.put("workspaceId", config.getWorkspaceId());
        response.put("docId", config.getDocId());
        response.put("timelineConfig", parseJsonString(config.getTimelineConfig()));
        response.put("displayConfig", parseJsonString(config.getDisplayConfig()));
        response.put("workingCalendar", parseJsonString(config.getWorkingCalendar()));
        response.put("createdAt", config.getCreatedAt());
        response.put("updatedAt", config.getUpdatedAt());
        return response;
    }
    
    private Map<String, Object> buildDependencyResponse(GanttTaskDependency dependency) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", dependency.getId());
        response.put("fromTaskId", dependency.getFromTaskId());
        response.put("toTaskId", dependency.getToTaskId());
        response.put("dependencyType", dependency.getDependencyType().getValue());
        response.put("lagDays", dependency.getLagDays());
        response.put("isFlexible", dependency.getIsFlexible());
        response.put("description", dependency.getDescription());
        response.put("createdAt", dependency.getCreatedAt());
        response.put("updatedAt", dependency.getUpdatedAt());
        return response;
    }
    
    private Object parseJsonString(String jsonStr) {
        if (jsonStr == null) {
            return null;
        }
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readValue(jsonStr, Object.class);
        } catch (Exception e) {
            return jsonStr; // 如果解析失败，返回原字符串
        }
    }
    
    // ============ 请求数据类 ============
    
    record CreateGanttConfigRequest(
        String timelineConfig,
        String displayConfig,
        String workingCalendar
    ) {}
    
    record UpdateGanttConfigRequest(
        String timelineConfig,
        String displayConfig,
        String workingCalendar
    ) {}
    
    record CreateDependencyRequest(
        @NotBlank String fromTaskId,
        @NotBlank String toTaskId,
        @NotBlank String dependencyType,
        Integer lagDays,
        Boolean isFlexible
    ) {}
    
    record BatchCreateDependenciesRequest(
        @NotNull List<CreateDependencyRequest> dependencies
    ) {}
}