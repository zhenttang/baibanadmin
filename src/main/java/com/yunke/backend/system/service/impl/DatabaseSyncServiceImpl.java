package com.yunke.backend.system.service.impl;

import com.yunke.backend.system.service.DatabaseSyncService;
import com.yunke.backend.document.util.YjsUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Set;

/**
 * 数据库同步服务实现
 * 参考AFFiNE开源代码的数据库模式定义
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DatabaseSyncServiceImpl implements DatabaseSyncService {
    
    private final YjsUtils yjsUtils;
    
    // AFFiNE支持的数据库集合（参考前端schema.ts）
    private static final Set<String> SUPPORTED_COLLECTIONS = Set.of(
        "folders",              // 文件夹结构
        "docProperties",        // 文档属性（主题、模式等）
        "docCustomPropertyInfo", // 自定义属性定义
        "pinnedCollections"     // 置顶集合
    );
    
    @Override
    public Mono<ResponseEntity<byte[]>> handleDatabaseSync(String workspaceId, String collectionName, String userId) {
        log.info("🗄️🗄️🗄️ [DB-SYNC] 处理数据库同步: workspaceId='{}', collection='{}', userId='{}'", 
                workspaceId, collectionName, userId);
        
        return Mono.fromCallable(() -> {
            if (!isCollectionSupported(collectionName)) {
                log.warn("🗄️ [DB-SYNC] 不支持的集合: {}", collectionName);
                return createUnsupportedCollectionResponse(collectionName);
            }
            
            // 根据集合类型返回相应的数据
            byte[] documentData = switch (collectionName) {
                case "docProperties" -> createDocPropertiesDocument(workspaceId);
                case "docCustomPropertyInfo" -> createDocCustomPropertyInfoDocument(workspaceId);
                case "folders" -> createFoldersDocument(workspaceId);
                case "pinnedCollections" -> createPinnedCollectionsDocument(workspaceId);
                default -> createEmptyDocument(collectionName);
            };
            
            log.info("🗄️ [DB-SYNC] 集合同步成功: collection='{}', docSize={} 字节", 
                    collectionName, documentData.length);
            
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .header("X-Doc-Type", "db-sync")
                    .header("X-Collection", collectionName)
                    .header("X-Doc-Size", String.valueOf(documentData.length))
                    .header("Cache-Control", "public, max-age=60")
                    .body(documentData);
        })
        .doOnError(error -> {
            log.error("🗄️ [DB-SYNC] 数据库同步失败: collection='{}', error={}", 
                    collectionName, error.getMessage(), error);
        });
    }
    
    @Override
    public Mono<ResponseEntity<byte[]>> handleDatabaseSyncDiff(String workspaceId, String collectionName, 
                                                               byte[] stateVector, String userId) {
        log.info("🗄️ [DB-SYNC-DIFF] 处理数据库同步差异: workspaceId='{}', collection='{}', stateVectorSize={}", 
                workspaceId, collectionName, stateVector != null ? stateVector.length : 0);
        
        return Mono.fromCallable(() -> {
            if (!isCollectionSupported(collectionName)) {
                return createUnsupportedCollectionResponse(collectionName);
            }
            
            // 对于数据库同步，通常没有增量更新，直接返回无变化
            // 实际项目中可能需要实现真正的差异计算
            log.debug("🗄️ [DB-SYNC-DIFF] 数据库集合无差异更新: collection='{}'", collectionName);
            
            return ResponseEntity.noContent()
                    .header("X-Doc-No-Changes", "true")
                    .header("X-Collection", collectionName)
                    .build();
        });
    }
    
    @Override
    public boolean isCollectionSupported(String collectionName) {
        return SUPPORTED_COLLECTIONS.contains(collectionName);
    }
    
    @Override
    public String[] getSupportedCollections() {
        return SUPPORTED_COLLECTIONS.toArray(new String[0]);
    }
    
    /**
     * 创建文档属性文档
     * 对应AFFiNE schema中的docProperties集合
     */
    private byte[] createDocPropertiesDocument(String workspaceId) {
        log.debug("🗄️ [DB-SYNC] 创建docProperties文档: workspaceId='{}'", workspaceId);
        
        // docProperties是t.document类型，包含文档的属性信息
        // 如primaryMode, edgelessColorTheme, journal, pageWidth等
        return yjsUtils.createMinimalValidYjsDoc("docProperties");
    }
    
    /**
     * 创建自定义属性信息文档
     * 对应AFFiNE schema中的docCustomPropertyInfo集合
     */
    private byte[] createDocCustomPropertyInfoDocument(String workspaceId) {
        log.debug("🗄️ [DB-SYNC] 创建docCustomPropertyInfo文档: workspaceId='{}'", workspaceId);
        
        // docCustomPropertyInfo包含自定义属性的定义
        // 如name, type, show, index, icon等
        return yjsUtils.createMinimalValidYjsDoc("docCustomPropertyInfo");
    }
    
    /**
     * 创建文件夹文档
     * 对应AFFiNE schema中的folders集合
     */
    private byte[] createFoldersDocument(String workspaceId) {
        log.debug("🗄️ [DB-SYNC] 创建folders文档: workspaceId='{}'", workspaceId);
        
        // folders包含文件夹结构信息
        // 如id, parentId, data, type, index等
        return yjsUtils.createMinimalValidYjsDoc("folders");
    }
    
    /**
     * 创建置顶集合文档
     * 对应AFFiNE schema中的pinnedCollections集合
     */
    private byte[] createPinnedCollectionsDocument(String workspaceId) {
        log.debug("🗄️ [DB-SYNC] 创建pinnedCollections文档: workspaceId='{}'", workspaceId);
        
        // pinnedCollections包含置顶的集合信息
        // 如collectionId, index等
        return yjsUtils.createMinimalValidYjsDoc("pinnedCollections");
    }
    
    /**
     * 创建空文档（用于未知集合）
     */
    private byte[] createEmptyDocument(String collectionName) {
        log.debug("🗄️ [DB-SYNC] 创建空文档: collection='{}'", collectionName);
        return yjsUtils.createMinimalValidYjsDoc(collectionName);
    }
    
    /**
     * 创建不支持集合的错误响应
     */
    private ResponseEntity<byte[]> createUnsupportedCollectionResponse(String collectionName) {
        String errorJson = String.format(
            "{\"error\":\"Unsupported collection\",\"collection\":\"%s\",\"supported\":[%s]}", 
            collectionName, 
            String.join(",", SUPPORTED_COLLECTIONS.stream()
                    .map(s -> "\"" + s + "\"")
                    .toArray(String[]::new))
        );
        
        return ResponseEntity.status(404)
                .header("X-Error", "Unsupported collection")
                .header("X-Collection", collectionName)
                .contentType(MediaType.APPLICATION_JSON)
                .body(errorJson.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}