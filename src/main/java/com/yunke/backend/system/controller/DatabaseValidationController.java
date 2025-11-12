package com.yunke.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.*;

/**
 * 数据库验证控制器 - 用于调试数据存储问题
 * 提供代码接口形式的数据库状态检查
 */
@RestController
@RequestMapping("/api/debug")
@RequiredArgsConstructor
@Slf4j
public class DatabaseValidationController {
    
    private final JdbcTemplate jdbcTemplate;
    
    /**
     * 完整数据库状态检查
     */
    @GetMapping("/database-status")
    public ResponseEntity<Map<String, Object>> getDatabaseStatus() {
        log.info("🔍📊 [DB-VALIDATION-001] 开始执行完整数据库状态检查");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 1. 基础表统计
            log.info("🔍📊 [DB-VALIDATION-002] 检查基础表记录数量");
            Map<String, Object> tableStats = getTableStatistics();
            result.put("tableStatistics", tableStats);
            
            // 2. 工作空间数据
            log.info("🔍📊 [DB-VALIDATION-003] 检查工作空间数据");
            Map<String, Object> workspaceData = getWorkspaceData();
            result.put("workspaceData", workspaceData);
            
            // 3. 文档数据
            log.info("🔍📊 [DB-VALIDATION-004] 检查文档数据");
            Map<String, Object> documentData = getDocumentData();
            result.put("documentData", documentData);
            
            // 4. 数据一致性检查
            log.info("🔍📊 [DB-VALIDATION-005] 执行数据一致性检查");
            Map<String, Object> consistencyCheck = getConsistencyCheck();
            result.put("consistencyCheck", consistencyCheck);
            
            // 5. 最近活动
            log.info("🔍📊 [DB-VALIDATION-006] 检查最近24小时活动");
            Map<String, Object> recentActivity = getRecentActivity();
            result.put("recentActivity", recentActivity);
            
            result.put("success", true);
            result.put("timestamp", System.currentTimeMillis());
            result.put("message", "数据库状态检查完成");
            
            log.info("✅📊 [DB-VALIDATION-007] 数据库状态检查成功完成");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌📊 [DB-VALIDATION-008] 数据库状态检查失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 获取表统计信息
     */
    private Map<String, Object> getTableStatistics() {
        Map<String, Object> stats = new HashMap<>();
        
        try {
            // 检查各个表的记录数
            String[] tables = {
                "workspaces", "workspace_pages", "doc_snapshots", 
                "doc_updates", "snapshots", "updates", "users"
            };
            
            for (String table : tables) {
                try {
                    Integer count = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM " + table, Integer.class);
                    stats.put(table, count != null ? count : 0);
                    log.debug("🔍📊 [DB-VALIDATION-002-{}] 表 {} 记录数: {}", table.toUpperCase(), table, count);
                } catch (Exception e) {
                    log.warn("⚠️📊 [DB-VALIDATION-002-{}] 无法查询表 {}: {}", table.toUpperCase(), table, e.getMessage());
                    stats.put(table, "ERROR: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("❌📊 [DB-VALIDATION-002-ERROR] 获取表统计失败", e);
            stats.put("error", e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * 获取工作空间数据
     */
    private Map<String, Object> getWorkspaceData() {
        Map<String, Object> data = new HashMap<>();
        
        try {
            // 最近创建的工作空间
            List<Map<String, Object>> recentWorkspaces = jdbcTemplate.queryForList(
                "SELECT id, name, `public`, created_at, created_by, enable_ai, indexed " +
                "FROM workspaces ORDER BY created_at DESC LIMIT 10");
            data.put("recentWorkspaces", recentWorkspaces);
            
            log.info("🔍📊 [DB-VALIDATION-003-WORKSPACES] 找到 {} 个最近工作空间", recentWorkspaces.size());
            
            // 工作空间与文档关联统计
            List<Map<String, Object>> workspaceDocCounts = jdbcTemplate.queryForList(
                "SELECT w.id AS workspace_id, w.name AS workspace_name, " +
                "w.created_at AS workspace_created, COUNT(wp.page_id) AS doc_count " +
                "FROM workspaces w LEFT JOIN workspace_pages wp ON w.id = wp.workspace_id " +
                "GROUP BY w.id, w.name, w.created_at ORDER BY w.created_at DESC");
            data.put("workspaceDocCounts", workspaceDocCounts);
            
            log.info("🔍📊 [DB-VALIDATION-003-COUNTS] 工作空间文档统计完成");
            
        } catch (Exception e) {
            log.error("❌📊 [DB-VALIDATION-003-ERROR] 获取工作空间数据失败", e);
            data.put("error", e.getMessage());
        }
        
        return data;
    }
    
    /**
     * 获取文档数据
     */
    private Map<String, Object> getDocumentData() {
        Map<String, Object> data = new HashMap<>();
        
        try {
            // 最近的文档页面
            List<Map<String, Object>> recentPages = jdbcTemplate.queryForList(
                "SELECT workspace_id, page_id AS doc_id, title, `public`, mode, " +
                "created_at, updated_at, blocked FROM workspace_pages " +
                "ORDER BY created_at DESC LIMIT 10");
            data.put("recentPages", recentPages);
            
            log.info("🔍📊 [DB-VALIDATION-004-PAGES] 找到 {} 个最近文档页面", recentPages.size());
            
            // 最近的文档快照
            try {
                List<Map<String, Object>> recentSnapshots = jdbcTemplate.queryForList(
                    "SELECT id, space_id, doc_id, timestamp, editor_id, " +
                    "LENGTH(bin) AS bin_size_bytes, created_at, updated_at " +
                    "FROM doc_snapshots ORDER BY created_at DESC LIMIT 10");
                data.put("recentSnapshots", recentSnapshots);
                log.info("🔍📊 [DB-VALIDATION-004-SNAPSHOTS] 找到 {} 个最近文档快照", recentSnapshots.size());
            } catch (Exception e) {
                log.warn("⚠️📊 [DB-VALIDATION-004-SNAPSHOTS] 新版快照表查询失败: {}", e.getMessage());
                data.put("recentSnapshots", "ERROR: " + e.getMessage());
            }
            
            // 最近的文档更新
            try {
                List<Map<String, Object>> recentUpdates = jdbcTemplate.queryForList(
                    "SELECT id, space_id, doc_id, timestamp, editor_id, merged, " +
                    "LENGTH(bin) AS bin_size_bytes, created_at FROM doc_updates " +
                    "ORDER BY created_at DESC LIMIT 10");
                data.put("recentUpdates", recentUpdates);
                log.info("🔍📊 [DB-VALIDATION-004-UPDATES] 找到 {} 个最近文档更新", recentUpdates.size());
            } catch (Exception e) {
                log.warn("⚠️📊 [DB-VALIDATION-004-UPDATES] 新版更新表查询失败: {}", e.getMessage());
                data.put("recentUpdates", "ERROR: " + e.getMessage());
            }
            
            // 旧版快照和更新
            try {
                List<Map<String, Object>> legacySnapshots = jdbcTemplate.queryForList(
                    "SELECT workspace_id, id AS doc_id, LENGTH(blob) AS blob_size_bytes, " +
                    "LENGTH(state) AS state_size_bytes, seq, created_at, updated_at, " +
                    "created_by, updated_by FROM snapshots ORDER BY updated_at DESC LIMIT 5");
                data.put("legacySnapshots", legacySnapshots);
                log.info("🔍📊 [DB-VALIDATION-004-LEGACY-SNAP] 找到 {} 个旧版快照", legacySnapshots.size());
            } catch (Exception e) {
                log.warn("⚠️📊 [DB-VALIDATION-004-LEGACY-SNAP] 旧版快照查询失败: {}", e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("❌📊 [DB-VALIDATION-004-ERROR] 获取文档数据失败", e);
            data.put("error", e.getMessage());
        }
        
        return data;
    }
    
    /**
     * 数据一致性检查
     */
    private Map<String, Object> getConsistencyCheck() {
        Map<String, Object> checks = new HashMap<>();
        
        try {
            // 检查workspace_pages但没有对应快照的文档
            Integer pagesWithoutSnapshots = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workspace_pages wp " +
                "LEFT JOIN doc_snapshots ds ON wp.workspace_id = ds.space_id AND wp.page_id = ds.doc_id " +
                "WHERE ds.id IS NULL", Integer.class);
            checks.put("pagesWithoutSnapshots", pagesWithoutSnapshots);
            log.info("🔍📊 [DB-VALIDATION-005-CONSISTENCY-1] 无快照的文档页面: {}", pagesWithoutSnapshots);
            
            // 检查快照但没有对应workspace_pages的文档
            try {
                Integer snapshotsWithoutPages = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM doc_snapshots ds " +
                    "LEFT JOIN workspace_pages wp ON ds.space_id = wp.workspace_id AND ds.doc_id = wp.page_id " +
                    "WHERE wp.page_id IS NULL", Integer.class);
                checks.put("snapshotsWithoutPages", snapshotsWithoutPages);
                log.info("🔍📊 [DB-VALIDATION-005-CONSISTENCY-2] 无页面记录的快照: {}", snapshotsWithoutPages);
            } catch (Exception e) {
                log.warn("⚠️📊 [DB-VALIDATION-005-CONSISTENCY-2] 快照一致性检查失败: {}", e.getMessage());
                checks.put("snapshotsWithoutPages", "ERROR: " + e.getMessage());
            }
            
            // 检查未合并的更新数量
            try {
                Integer unmergedUpdates = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM doc_updates WHERE merged = false", Integer.class);
                checks.put("unmergedUpdates", unmergedUpdates);
                log.info("🔍📊 [DB-VALIDATION-005-CONSISTENCY-3] 未合并的更新: {}", unmergedUpdates);
            } catch (Exception e) {
                log.warn("⚠️📊 [DB-VALIDATION-005-CONSISTENCY-3] 更新合并状态检查失败: {}", e.getMessage());
                checks.put("unmergedUpdates", "ERROR: " + e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("❌📊 [DB-VALIDATION-005-ERROR] 一致性检查失败", e);
            checks.put("error", e.getMessage());
        }
        
        return checks;
    }
    
    /**
     * 获取最近活动
     */
    private Map<String, Object> getRecentActivity() {
        Map<String, Object> activity = new HashMap<>();
        
        try {
            // 最近24小时的工作空间创建
            Integer workspacesCreated24h = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workspaces WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)", 
                Integer.class);
            activity.put("workspacesCreated24h", workspacesCreated24h);
            log.info("🔍📊 [DB-VALIDATION-006-ACTIVITY-1] 24小时内创建工作空间: {}", workspacesCreated24h);
            
            // 最近24小时的文档创建
            Integer docsCreated24h = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM workspace_pages WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)", 
                Integer.class);
            activity.put("docsCreated24h", docsCreated24h);
            log.info("🔍📊 [DB-VALIDATION-006-ACTIVITY-2] 24小时内创建文档: {}", docsCreated24h);
            
            // 最近24小时的文档更新
            try {
                Integer docUpdates24h = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM doc_updates WHERE created_at >= DATE_SUB(NOW(), INTERVAL 24 HOUR)", 
                    Integer.class);
                activity.put("docUpdates24h", docUpdates24h);
                log.info("🔍📊 [DB-VALIDATION-006-ACTIVITY-3] 24小时内文档更新: {}", docUpdates24h);
            } catch (Exception e) {
                log.warn("⚠️📊 [DB-VALIDATION-006-ACTIVITY-3] 文档更新统计失败: {}", e.getMessage());
                activity.put("docUpdates24h", "ERROR: " + e.getMessage());
            }
            
        } catch (Exception e) {
            log.error("❌📊 [DB-VALIDATION-006-ERROR] 获取最近活动失败", e);
            activity.put("error", e.getMessage());
        }
        
        return activity;
    }
    
    /**
     * 检查特定工作空间的数据
     */
    @GetMapping("/workspace/{workspaceId}/status")
    public ResponseEntity<Map<String, Object>> getWorkspaceStatus(@PathVariable String workspaceId) {
        log.info("🔍🏢 [WORKSPACE-VALIDATION-001] 开始检查工作空间状态: {}", workspaceId);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 工作空间基本信息
            List<Map<String, Object>> workspaceInfo = jdbcTemplate.queryForList(
                "SELECT * FROM workspaces WHERE id = ?", workspaceId);
            result.put("workspaceInfo", workspaceInfo);
            
            if (workspaceInfo.isEmpty()) {
                log.warn("⚠️🏢 [WORKSPACE-VALIDATION-002] 工作空间不存在: {}", workspaceId);
                result.put("exists", false);
            } else {
                log.info("✅🏢 [WORKSPACE-VALIDATION-003] 工作空间存在: {}", workspaceId);
                result.put("exists", true);
                
                // 该工作空间的文档
                List<Map<String, Object>> docs = jdbcTemplate.queryForList(
                    "SELECT * FROM workspace_pages WHERE workspace_id = ? ORDER BY created_at DESC", 
                    workspaceId);
                result.put("documents", docs);
                log.info("📄🏢 [WORKSPACE-VALIDATION-004] 工作空间文档数量: {}", docs.size());
                
                // 该工作空间的快照
                try {
                    List<Map<String, Object>> snapshots = jdbcTemplate.queryForList(
                        "SELECT id, doc_id, timestamp, editor_id, LENGTH(bin) as bin_size, created_at " +
                        "FROM doc_snapshots WHERE space_id = ? ORDER BY created_at DESC", 
                        workspaceId);
                    result.put("snapshots", snapshots);
                    log.info("📊🏢 [WORKSPACE-VALIDATION-005] 工作空间快照数量: {}", snapshots.size());
                } catch (Exception e) {
                    log.warn("⚠️🏢 [WORKSPACE-VALIDATION-005] 快照查询失败: {}", e.getMessage());
                    result.put("snapshots", "ERROR: " + e.getMessage());
                }
                
                // 该工作空间的更新
                try {
                    List<Map<String, Object>> updates = jdbcTemplate.queryForList(
                        "SELECT id, doc_id, timestamp, editor_id, merged, LENGTH(bin) as bin_size, created_at " +
                        "FROM doc_updates WHERE space_id = ? ORDER BY created_at DESC LIMIT 20", 
                        workspaceId);
                    result.put("updates", updates);
                    log.info("🔄🏢 [WORKSPACE-VALIDATION-006] 工作空间更新数量: {}", updates.size());
                } catch (Exception e) {
                    log.warn("⚠️🏢 [WORKSPACE-VALIDATION-006] 更新查询失败: {}", e.getMessage());
                    result.put("updates", "ERROR: " + e.getMessage());
                }
            }
            
            result.put("success", true);
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("✅🏢 [WORKSPACE-VALIDATION-007] 工作空间状态检查完成: {}", workspaceId);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌🏢 [WORKSPACE-VALIDATION-008] 工作空间状态检查失败: {}", workspaceId, e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(result);
        }
    }
    
    /**
     * 存储空间使用情况
     */
    @GetMapping("/storage-usage")
    public ResponseEntity<Map<String, Object>> getStorageUsage() {
        log.info("🔍💾 [STORAGE-VALIDATION-001] 开始检查存储空间使用情况");
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<Map<String, Object>> storageStats = new ArrayList<>();
            
            // 检查各个表的存储使用情况
            String[] tables = {"doc_snapshots", "doc_updates", "snapshots", "updates"};
            String[] blobColumns = {"bin", "bin", "blob", "blob"};
            
            for (int i = 0; i < tables.length; i++) {
                try {
                    Map<String, Object> tableStat = jdbcTemplate.queryForMap(
                        "SELECT '" + tables[i] + "' as table_name, " +
                        "COUNT(*) as record_count, " +
                        "ROUND(SUM(LENGTH(" + blobColumns[i] + "))/1024/1024, 2) as size_mb " +
                        "FROM " + tables[i]);
                    storageStats.add(tableStat);
                    
                    log.info("💾 [STORAGE-VALIDATION-002-{}] 表 {}: {} 记录, {} MB", 
                            tables[i].toUpperCase(), tables[i], 
                            tableStat.get("record_count"), tableStat.get("size_mb"));
                } catch (Exception e) {
                    log.warn("⚠️💾 [STORAGE-VALIDATION-002-{}] 表 {} 存储统计失败: {}", 
                            tables[i].toUpperCase(), tables[i], e.getMessage());
                    Map<String, Object> errorStat = new HashMap<>();
                    errorStat.put("table_name", tables[i]);
                    errorStat.put("error", e.getMessage());
                    storageStats.add(errorStat);
                }
            }
            
            result.put("storageStats", storageStats);
            result.put("success", true);
            result.put("timestamp", System.currentTimeMillis());
            
            log.info("✅💾 [STORAGE-VALIDATION-003] 存储使用情况检查完成");
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("❌💾 [STORAGE-VALIDATION-004] 存储使用情况检查失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            result.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(500).body(result);
        }
    }
}