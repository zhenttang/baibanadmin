package com.yunke.backend.workspace.service;

import java.util.Optional;

/**
 * 工作空间ID映射服务接口
 * 处理AFFiNE前端的双重ID系统：
 * - 短格式ID (nanoid): 用于HTTP API请求
 * - UUID格式: 用于WebSocket连接和内部存储
 */
public interface WorkspaceIdMappingService {
    
    /**
     * 将短格式ID转换为UUID格式
     * @param shortId 短格式ID (如: jqTFqzCl20mSOklIJnvKb)
     * @return UUID格式ID (如: 09056edd-a08a-40b5-a420-0205a8514ff2)
     */
    Optional<String> shortIdToUuid(String shortId);
    
    /**
     * 将UUID格式转换为短格式ID
     * @param uuid UUID格式ID (如: 09056edd-a08a-40b5-a420-0205a8514ff2)
     * @return 短格式ID (如: jqTFqzCl20mSOklIJnvKb)
     */
    Optional<String> uuidToShortId(String uuid);
    
    /**
     * 创建新的ID映射关系
     * @param shortId 短格式ID
     * @param uuid UUID格式ID
     * @return 创建成功返回true
     */
    boolean createMapping(String shortId, String uuid);
    
    /**
     * 检查ID是否为短格式（nanoid）
     * @param id 待检查的ID
     * @return 是短格式返回true
     */
    boolean isShortFormat(String id);
    
    /**
     * 检查ID是否为UUID格式
     * @param id 待检查的ID
     * @return 是UUID格式返回true
     */
    boolean isUuidFormat(String id);
    
    /**
     * 智能转换ID：根据输入格式自动转换到对应格式
     * 如果输入是短格式，返回UUID格式；反之亦然
     * @param id 待转换的ID
     * @return 转换后的ID，如果无映射则返回原ID
     */
    String smartConvert(String id);
    
    /**
     * 获取真实的工作空间ID（优先返回UUID格式）
     * @param inputId 输入的ID（可能是短格式或UUID格式）
     * @return 真实的工作空间ID（UUID格式）
     */
    String getRealWorkspaceId(String inputId);
}