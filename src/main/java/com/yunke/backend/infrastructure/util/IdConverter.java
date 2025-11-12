package com.yunke.backend.infrastructure.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * AFFiNE兼容的ID转换器
 * 完全参考AFFiNE开源代码的id-converter.ts实现
 * 
 * 主要功能：
 * - 新格式ID转旧格式ID (前端 -> 后端存储)
 * - 旧格式ID转新格式ID (后端存储 -> 前端)
 * - 支持数据库同步和用户数据格式
 */
@Slf4j
@Component
public class IdConverter {
    
    // 缓存转换结果以提高性能
    private final Map<String, Map<String, String>> oldIdToNewIdCache = new ConcurrentHashMap<>();
    private final Map<String, Map<String, String>> newIdToOldIdCache = new ConcurrentHashMap<>();
    
    // 正则表达式模式
    private static final Pattern USER_DATA_PATTERN = Pattern.compile("^(userdata\\$[\\w-]+)\\$([^\\$]+)");
    
    /**
     * 新格式ID转旧格式ID (前端请求 -> 后端存储)
     * 
     * @param newId 新格式ID (如: db$docProperties)
     * @param spaceId 工作空间ID
     * @return 旧格式ID (如: db$${spaceId}$docProperties)
     */
    public String newIdToOldId(String newId, String spaceId) {
        if (newId == null || spaceId == null) {
            return newId;
        }
        
        // 缓存查找
        String cached = getCachedNewToOld(spaceId, newId);
        if (cached != null) {
            return cached;
        }
        
        String result = convertNewIdToOldId(newId, spaceId);
        
        // 缓存结果
        cacheNewToOld(spaceId, newId, result);
        
        log.debug("🔄 [ID-CONVERT] New->Old: '{}' -> '{}' (spaceId: {})", newId, result, spaceId);
        return result;
    }
    
    /**
     * 旧格式ID转新格式ID (后端存储 -> 前端响应)
     * 
     * @param oldId 旧格式ID (如: db$${spaceId}$docProperties)
     * @param spaceId 工作空间ID
     * @return 新格式ID (如: db$docProperties)
     */
    public String oldIdToNewId(String oldId, String spaceId) {
        if (oldId == null || spaceId == null) {
            return oldId;
        }
        
        // 缓存查找
        String cached = getCachedOldToNew(spaceId, oldId);
        if (cached != null) {
            return cached;
        }
        
        String result = convertOldIdToNewId(oldId, spaceId);
        
        // 缓存结果
        cacheOldToNew(spaceId, oldId, result);
        
        log.debug("🔄 [ID-CONVERT] Old->New: '{}' -> '{}' (spaceId: {})", oldId, result, spaceId);
        return result;
    }
    
    /**
     * 核心转换逻辑：新格式 -> 旧格式
     */
    private String convertNewIdToOldId(String newId, String spaceId) {
        // 处理数据库同步格式: db$docId -> db$${spaceId}$docId
        if (newId.startsWith("db$")) {
            return newId.replace("db$", "db$" + spaceId + "$");
        }
        
        // 处理用户数据格式: userdata$userId$docId -> userdata$userId$spaceId$docId
        if (newId.startsWith("userdata$")) {
            return USER_DATA_PATTERN.matcher(newId).replaceFirst("$1$" + spaceId + "$$2");
        }
        
        // 其他格式保持不变（可能需要根据具体的文档映射表进行转换）
        return newId;
    }
    
    /**
     * 核心转换逻辑：旧格式 -> 新格式
     */
    private String convertOldIdToNewId(String oldId, String spaceId) {
        // 处理数据库同步格式: db$${spaceId}$docId -> db$docId
        String dbPrefix = "db$" + spaceId + "$";
        if (oldId.startsWith(dbPrefix)) {
            return oldId.replace(dbPrefix, "db$");
        }
        
        // 处理用户数据格式: userdata$userId$spaceId$docId -> userdata$userId$docId
        String userDataPattern = "userdata$([\\w-]+)$" + Pattern.quote(spaceId) + "$";
        if (oldId.matches(userDataPattern + ".*")) {
            return oldId.replaceFirst("\\$" + Pattern.quote(spaceId) + "\\$", "$");
        }
        
        // 其他格式保持不变
        return oldId;
    }
    
    /**
     * 检查ID是否为数据库同步格式
     */
    public boolean isDatabaseSyncId(String id) {
        return id != null && id.startsWith("db$");
    }
    
    /**
     * 检查ID是否为用户数据格式
     */
    public boolean isUserDataId(String id) {
        return id != null && id.startsWith("userdata$");
    }
    
    /**
     * 从数据库同步ID中提取集合名称
     */
    public String extractCollectionName(String dbSyncId) {
        if (!isDatabaseSyncId(dbSyncId)) {
            throw new IllegalArgumentException("Not a database sync ID: " + dbSyncId);
        }
        
        // 处理新格式: db$collectionName
        if (dbSyncId.startsWith("db$") && dbSyncId.indexOf("$", 3) == -1) {
            return dbSyncId.substring(3);
        }
        
        // 处理旧格式: db$workspaceId$collectionName
        String[] parts = dbSyncId.split("\\$");
        if (parts.length >= 3) {
            return parts[2];
        }
        
        throw new IllegalArgumentException("Invalid database sync ID format: " + dbSyncId);
    }
    
    /**
     * 从用户数据ID中提取用户ID和集合名称
     */
    public UserDataInfo extractUserDataInfo(String userDataId) {
        if (!isUserDataId(userDataId)) {
            throw new IllegalArgumentException("Not a user data ID: " + userDataId);
        }
        
        String[] parts = userDataId.split("\\$");
        if (parts.length >= 3) {
            String userId = parts[1];
            String collectionName = parts[parts.length - 1];
            return new UserDataInfo(userId, collectionName);
        }
        
        throw new IllegalArgumentException("Invalid user data ID format: " + userDataId);
    }
    
    /**
     * 用户数据信息
     */
    public static class UserDataInfo {
        public final String userId;
        public final String collectionName;
        
        public UserDataInfo(String userId, String collectionName) {
            this.userId = userId;
            this.collectionName = collectionName;
        }
    }
    
    // 缓存管理方法
    private String getCachedNewToOld(String spaceId, String newId) {
        return newIdToOldIdCache.computeIfAbsent(spaceId, k -> new HashMap<>()).get(newId);
    }
    
    private void cacheNewToOld(String spaceId, String newId, String oldId) {
        newIdToOldIdCache.computeIfAbsent(spaceId, k -> new HashMap<>()).put(newId, oldId);
    }
    
    private String getCachedOldToNew(String spaceId, String oldId) {
        return oldIdToNewIdCache.computeIfAbsent(spaceId, k -> new HashMap<>()).get(oldId);
    }
    
    private void cacheOldToNew(String spaceId, String oldId, String newId) {
        oldIdToNewIdCache.computeIfAbsent(spaceId, k -> new HashMap<>()).put(oldId, newId);
    }
    
    /**
     * 清除指定工作空间的缓存
     */
    public void clearCache(String spaceId) {
        oldIdToNewIdCache.remove(spaceId);
        newIdToOldIdCache.remove(spaceId);
        log.debug("🗑️ [ID-CONVERT] Cleared cache for spaceId: {}", spaceId);
    }
    
    /**
     * 清除所有缓存
     */
    public void clearAllCache() {
        oldIdToNewIdCache.clear();
        newIdToOldIdCache.clear();
        log.debug("🗑️ [ID-CONVERT] Cleared all ID conversion cache");
    }
    
    /**
     * 获取缓存统计信息
     */
    public String getCacheStats() {
        int oldToNewSpaces = oldIdToNewIdCache.size();
        int newToOldSpaces = newIdToOldIdCache.size();
        int totalOldToNew = oldIdToNewIdCache.values().stream().mapToInt(Map::size).sum();
        int totalNewToOld = newIdToOldIdCache.values().stream().mapToInt(Map::size).sum();
        
        return String.format("Cache Stats - Spaces(O->N: %d, N->O: %d), Entries(O->N: %d, N->O: %d)", 
                oldToNewSpaces, newToOldSpaces, totalOldToNew, totalNewToOld);
    }
}

