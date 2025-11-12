package com.yunke.backend.infrastructure.util;

import com.yunke.backend.workspace.service.WorkspaceIdMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.regex.Pattern;

/**
 * 工作空间ID转换工具类
 * 统一处理短格式ID到UUID格式的转换，确保后端查询的一致性
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WorkspaceIdConverter {
    
    private final WorkspaceIdMappingService mappingService;
    
    // ID格式正则表达式
    private static final Pattern NANOID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{21}$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    
    /**
     * 统一ID转换方法 - 确保返回数据库格式的UUID
     * 这是全局使用的核心方法
     * 
     * @param inputId 输入的ID（可能是短格式或UUID格式）
     * @return 数据库格式的UUID，如果转换失败返回原ID
     */
    public String toUuidFormat(String inputId) {
        if (inputId == null || inputId.trim().isEmpty()) {
            return inputId;
        }
        
        String trimmedId = inputId.trim();
        
        // 如果已经是UUID格式，直接返回
        if (isUuidFormat(trimmedId)) {
            log.debug("🎯 [ID-CONVERTER] 输入已是UUID格式: '{}'", trimmedId);
            return trimmedId;
        }
        
        // 如果是短格式，尝试转换为UUID
        if (isShortFormat(trimmedId)) {
            log.debug("🔄 [ID-CONVERTER] 检测到短格式ID，开始转换: '{}'", trimmedId);
            
            Optional<String> uuid = mappingService.shortIdToUuid(trimmedId);
            if (uuid.isPresent()) {
                log.info("✅ [ID-CONVERTER] 短格式ID转换成功: '{}' -> '{}'", trimmedId, uuid.get());
                return uuid.get();
            } else {
                log.warn("⚠️ [ID-CONVERTER] 未找到短格式ID的映射: '{}', 返回原值", trimmedId);
                return trimmedId;
            }
        }
        
        // 其他格式直接返回
        log.debug("➡️ [ID-CONVERTER] 未知格式ID，返回原值: '{}'", trimmedId);
        return trimmedId;
    }
    
    /**
     * 批量转换ID数组
     * 
     * @param inputIds 输入ID数组
     * @return 转换后的UUID格式ID数组
     */
    public String[] toUuidFormat(String[] inputIds) {
        if (inputIds == null) {
            return null;
        }
        
        String[] result = new String[inputIds.length];
        for (int i = 0; i < inputIds.length; i++) {
            result[i] = toUuidFormat(inputIds[i]);
        }
        return result;
    }
    
    /**
     * 工作空间ID转换 - 专用于工作空间相关操作
     * 
     * @param workspaceId 工作空间ID
     * @return UUID格式的工作空间ID
     */
    public String convertWorkspaceId(String workspaceId) {
        log.debug("🏢 [ID-CONVERTER] 工作空间ID转换: '{}'", workspaceId);
        String result = toUuidFormat(workspaceId);
        
        if (!result.equals(workspaceId)) {
            log.info("🏢 [ID-CONVERTER] 工作空间ID已转换: '{}' -> '{}'", workspaceId, result);
        }
        
        return result;
    }
    
    /**
     * 文档ID转换 - 专用于文档相关操作
     * 
     * @param docId 文档ID
     * @return UUID格式的文档ID（如果适用）
     */
    public String convertDocId(String docId) {
        log.debug("📄 [ID-CONVERTER] 文档ID转换: '{}'", docId);
        
        // 特殊文档ID格式不需要转换
        if (docId != null && (docId.startsWith("db$") || docId.startsWith("userdata$"))) {
            log.debug("📄 [ID-CONVERTER] 特殊文档ID格式，不转换: '{}'", docId);
            return docId;
        }
        
        String result = toUuidFormat(docId);
        
        if (!result.equals(docId)) {
            log.info("📄 [ID-CONVERTER] 文档ID已转换: '{}' -> '{}'", docId, result);
        }
        
        return result;
    }
    
    /**
     * 智能转换 - 根据上下文自动判断转换策略
     * 
     * @param id 输入ID
     * @param context 上下文信息（如 "workspace", "document", "user"）
     * @return 转换后的ID
     */
    public String smartConvert(String id, String context) {
        if (id == null) {
            return null;
        }
        
        log.debug("🧠 [ID-CONVERTER] 智能转换: id='{}', context='{}'", id, context);
        
        switch (context != null ? context.toLowerCase() : "") {
            case "workspace":
            case "space":
                return convertWorkspaceId(id);
            case "document":
            case "doc":
                return convertDocId(id);
            default:
                return toUuidFormat(id);
        }
    }
    
    /**
     * 检查ID是否为短格式（nanoid）
     */
    public boolean isShortFormat(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        return NANOID_PATTERN.matcher(id).matches();
    }
    
    /**
     * 检查ID是否为UUID格式
     */
    public boolean isUuidFormat(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        return UUID_PATTERN.matcher(id).matches();
    }
    
    /**
     * 获取ID格式类型
     * 
     * @param id 输入ID
     * @return "short", "uuid", "special", "unknown"
     */
    public String getIdFormat(String id) {
        if (id == null || id.isEmpty()) {
            return "empty";
        }
        
        if (isUuidFormat(id)) {
            return "uuid";
        } else if (isShortFormat(id)) {
            return "short";
        } else if (id.startsWith("db$") || id.startsWith("userdata$")) {
            return "special";
        } else {
            return "unknown";
        }
    }
    
    /**
     * 尝试建立ID映射关系
     * 当发现新的短格式和UUID格式配对时，自动创建映射
     * 
     * @param shortId 短格式ID
     * @param uuidId UUID格式ID
     * @return 是否成功建立映射
     */
    public boolean tryCreateMapping(String shortId, String uuidId) {
        if (!isShortFormat(shortId) || !isUuidFormat(uuidId)) {
            log.debug("🔗 [ID-CONVERTER] ID格式不匹配，无法创建映射: short='{}', uuid='{}'", shortId, uuidId);
            return false;
        }
        
        // 检查映射是否已存在
        Optional<String> existingUuid = mappingService.shortIdToUuid(shortId);
        if (existingUuid.isPresent()) {
            if (existingUuid.get().equals(uuidId)) {
                log.debug("🔗 [ID-CONVERTER] 映射已存在且正确: '{}' -> '{}'", shortId, uuidId);
                return true;
            } else {
                log.warn("🔗 [ID-CONVERTER] 映射冲突: '{}' 已映射到 '{}', 不能映射到 '{}'", 
                        shortId, existingUuid.get(), uuidId);
                return false;
            }
        }
        
        // 创建新映射
        boolean success = mappingService.createMapping(shortId, uuidId);
        if (success) {
            log.info("🔗 [ID-CONVERTER] 自动创建ID映射: '{}' <-> '{}'", shortId, uuidId);
        } else {
            log.warn("🔗 [ID-CONVERTER] 创建ID映射失败: '{}' <-> '{}'", shortId, uuidId);
        }
        
        return success;
    }
    
    /**
     * 从操作上下文中自动发现并创建ID映射
     * 当同时出现短格式和UUID格式ID时，尝试建立关联
     * 
     * @param possibleShortId 可能的短格式ID
     * @param possibleUuidId 可能的UUID格式ID
     */
    public void autoDiscoverMapping(String possibleShortId, String possibleUuidId) {
        if (possibleShortId == null || possibleUuidId == null) {
            return;
        }
        
        // 自动检测和交换参数
        String shortId = null;
        String uuidId = null;
        
        if (isShortFormat(possibleShortId) && isUuidFormat(possibleUuidId)) {
            shortId = possibleShortId;
            uuidId = possibleUuidId;
        } else if (isUuidFormat(possibleShortId) && isShortFormat(possibleUuidId)) {
            shortId = possibleUuidId;
            uuidId = possibleShortId;
        } else {
            log.debug("🔍 [ID-CONVERTER] 无法自动发现映射，ID格式不匹配: '{}', '{}'", 
                    possibleShortId, possibleUuidId);
            return;
        }
        
        log.debug("🔍 [ID-CONVERTER] 自动发现潜在映射: short='{}', uuid='{}'", shortId, uuidId);
        tryCreateMapping(shortId, uuidId);
    }
    
    /**
     * 验证转换结果
     * 用于调试和监控转换是否正确
     * 
     * @param originalId 原始ID
     * @param convertedId 转换后ID
     * @return 转换是否有效
     */
    public boolean validateConversion(String originalId, String convertedId) {
        if (originalId == null && convertedId == null) {
            return true;
        }
        
        if (originalId == null || convertedId == null) {
            return false;
        }
        
        // 如果没有转换，原ID应该是有效格式
        if (originalId.equals(convertedId)) {
            return isUuidFormat(originalId) || !isShortFormat(originalId);
        }
        
        // 如果有转换，应该是从短格式转到UUID格式
        return isShortFormat(originalId) && isUuidFormat(convertedId);
    }
}

