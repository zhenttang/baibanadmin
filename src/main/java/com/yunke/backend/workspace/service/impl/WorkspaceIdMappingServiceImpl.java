package com.yunke.backend.workspace.service.impl;

import com.yunke.backend.workspace.service.WorkspaceIdMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 工作空间ID映射服务实现
 * 使用Redis缓存映射关系，提高查询性能
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WorkspaceIdMappingServiceImpl implements WorkspaceIdMappingService {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    // Redis key前缀
    private static final String SHORT_TO_UUID_PREFIX = "ws:mapping:s2u:";
    private static final String UUID_TO_SHORT_PREFIX = "ws:mapping:u2s:";
    
    // ID格式正则表达式
    private static final Pattern NANOID_PATTERN = Pattern.compile("^[A-Za-z0-9_-]{21}$");
    private static final Pattern UUID_PATTERN = Pattern.compile("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$");
    
    // 缓存过期时间（7天）
    private static final long CACHE_EXPIRE_DAYS = 7;

    @Override
    public Optional<String> shortIdToUuid(String shortId) {
        if (shortId == null || shortId.isEmpty()) {
            return Optional.empty();
        }
        
        log.debug("🔄 [ID-MAPPING] 短格式ID转UUID: shortId='{}'", shortId);
        
        try {
            String uuid = redisTemplate.opsForValue().get(SHORT_TO_UUID_PREFIX + shortId);
            if (uuid != null) {
                log.debug("✅ [ID-MAPPING] 找到缓存映射: '{}' -> '{}'", shortId, uuid);
                return Optional.of(uuid);
            } else {
                log.debug("❌ [ID-MAPPING] 未找到映射: shortId='{}'", shortId);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("❌ [ID-MAPPING] 查询短格式ID映射失败: shortId='{}', error={}", shortId, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public Optional<String> uuidToShortId(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return Optional.empty();
        }
        
        log.debug("🔄 [ID-MAPPING] UUID转短格式ID: uuid='{}'", uuid);
        
        try {
            String shortId = redisTemplate.opsForValue().get(UUID_TO_SHORT_PREFIX + uuid);
            if (shortId != null) {
                log.debug("✅ [ID-MAPPING] 找到缓存映射: '{}' -> '{}'", uuid, shortId);
                return Optional.of(shortId);
            } else {
                log.debug("❌ [ID-MAPPING] 未找到映射: uuid='{}'", uuid);
                return Optional.empty();
            }
        } catch (Exception e) {
            log.error("❌ [ID-MAPPING] 查询UUID映射失败: uuid='{}', error={}", uuid, e.getMessage(), e);
            return Optional.empty();
        }
    }

    @Override
    public boolean createMapping(String shortId, String uuid) {
        if (shortId == null || shortId.isEmpty() || uuid == null || uuid.isEmpty()) {
            log.warn("⚠️ [ID-MAPPING] 创建映射失败: 参数为空 shortId='{}', uuid='{}'", shortId, uuid);
            return false;
        }
        
        // 验证ID格式
        if (!isShortFormat(shortId)) {
            log.warn("⚠️ [ID-MAPPING] 创建映射失败: shortId格式不正确 '{}'", shortId);
            return false;
        }
        
        if (!isUuidFormat(uuid)) {
            log.warn("⚠️ [ID-MAPPING] 创建映射失败: uuid格式不正确 '{}'", uuid);
            return false;
        }
        
        log.info("🔧 [ID-MAPPING] 创建新映射: '{}' <-> '{}'", shortId, uuid);
        
        try {
            // 创建双向映射
            redisTemplate.opsForValue().set(SHORT_TO_UUID_PREFIX + shortId, uuid, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            redisTemplate.opsForValue().set(UUID_TO_SHORT_PREFIX + uuid, shortId, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
            
            log.info("✅ [ID-MAPPING] 映射创建成功: '{}' <-> '{}'", shortId, uuid);
            return true;
        } catch (Exception e) {
            log.error("❌ [ID-MAPPING] 创建映射失败: shortId='{}', uuid='{}', error={}", 
                    shortId, uuid, e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean isShortFormat(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        return NANOID_PATTERN.matcher(id).matches();
    }

    @Override
    public boolean isUuidFormat(String id) {
        if (id == null || id.isEmpty()) {
            return false;
        }
        return UUID_PATTERN.matcher(id).matches();
    }

    @Override
    public String smartConvert(String id) {
        if (id == null || id.isEmpty()) {
            return id;
        }
        
        log.debug("🧠 [ID-MAPPING] 智能转换ID: '{}'", id);
        
        if (isShortFormat(id)) {
            // 短格式 -> UUID格式
            Optional<String> uuid = shortIdToUuid(id);
            if (uuid.isPresent()) {
                log.debug("🔄 [ID-MAPPING] 短格式转UUID: '{}' -> '{}'", id, uuid.get());
                return uuid.get();
            }
        } else if (isUuidFormat(id)) {
            // UUID格式 -> 短格式
            Optional<String> shortId = uuidToShortId(id);
            if (shortId.isPresent()) {
                log.debug("🔄 [ID-MAPPING] UUID转短格式: '{}' -> '{}'", id, shortId.get());
                return shortId.get();
            }
        } else {
            log.debug("⚠️ [ID-MAPPING] ID格式无法识别: '{}'", id);
        }
        
        // 无映射时返回原ID
        log.debug("➡️ [ID-MAPPING] 无映射，返回原ID: '{}'", id);
        return id;
    }

    @Override
    public String getRealWorkspaceId(String inputId) {
        if (inputId == null || inputId.isEmpty()) {
            return inputId;
        }
        
        log.debug("🎯 [ID-MAPPING] 获取真实工作空间ID: inputId='{}'", inputId);
        
        // 如果已经是UUID格式，直接返回
        if (isUuidFormat(inputId)) {
            log.debug("✅ [ID-MAPPING] 输入已是UUID格式: '{}'", inputId);
            return inputId;
        }
        
        // 如果是短格式，转换为UUID格式
        if (isShortFormat(inputId)) {
            Optional<String> uuid = shortIdToUuid(inputId);
            if (uuid.isPresent()) {
                log.debug("🔄 [ID-MAPPING] 短格式转换为UUID: '{}' -> '{}'", inputId, uuid.get());
                return uuid.get();
            } else {
                log.warn("⚠️ [ID-MAPPING] 未找到短格式ID的UUID映射: '{}'", inputId);
                return inputId;
            }
        }
        
        // 其他格式直接返回
        log.debug("➡️ [ID-MAPPING] 未知格式，返回原值: '{}'", inputId);
        return inputId;
    }
}