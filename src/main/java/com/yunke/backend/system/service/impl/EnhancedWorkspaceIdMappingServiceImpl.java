package com.yunke.backend.system.service.impl;

import com.yunke.backend.workspace.domain.entity.WorkspaceIdMapping;

import com.yunke.backend.workspace.repository.WorkspaceIdMappingRepository;
import com.yunke.backend.workspace.service.WorkspaceIdMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * 增强版工作空间ID映射服务实现
 * 使用Redis缓存 + 数据库持久化的双层存储策略
 */
@Service
@Primary
@RequiredArgsConstructor
@Slf4j
public class EnhancedWorkspaceIdMappingServiceImpl implements WorkspaceIdMappingService {
    
    private final RedisTemplate<String, String> redisTemplate;
    private final WorkspaceIdMappingRepository mappingRepository;
    
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
        
        // 1. 先查缓存
        try {
            String cachedUuid = redisTemplate.opsForValue().get(SHORT_TO_UUID_PREFIX + shortId);
            if (cachedUuid != null) {
                log.debug("✅ [ID-MAPPING] 缓存命中: '{}' -> '{}'", shortId, cachedUuid);
                return Optional.of(cachedUuid);
            }
        } catch (Exception e) {
            log.warn("⚠️ [ID-MAPPING] Redis缓存查询失败: {}", e.getMessage());
        }
        
        // 2. 查数据库
        try {
            Optional<String> dbUuid = mappingRepository.findUuidByShortId(shortId);
            if (dbUuid.isPresent()) {
                log.debug("💾 [ID-MAPPING] 数据库查询成功: '{}' -> '{}'", shortId, dbUuid.get());
                
                // 3. 回写缓存
                try {
                    redisTemplate.opsForValue().set(SHORT_TO_UUID_PREFIX + shortId, dbUuid.get(), 
                            CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                    redisTemplate.opsForValue().set(UUID_TO_SHORT_PREFIX + dbUuid.get(), shortId, 
                            CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                } catch (Exception e) {
                    log.warn("⚠️ [ID-MAPPING] 缓存回写失败: {}", e.getMessage());
                }
                
                return dbUuid;
            }
        } catch (Exception e) {
            log.error("❌ [ID-MAPPING] 数据库查询失败: shortId='{}', error={}", shortId, e.getMessage(), e);
        }
        
        log.debug("❌ [ID-MAPPING] 未找到映射: shortId='{}'", shortId);
        return Optional.empty();
    }

    @Override
    public Optional<String> uuidToShortId(String uuid) {
        if (uuid == null || uuid.isEmpty()) {
            return Optional.empty();
        }
        
        log.debug("🔄 [ID-MAPPING] UUID转短格式ID: uuid='{}'", uuid);
        
        // 1. 先查缓存
        try {
            String cachedShortId = redisTemplate.opsForValue().get(UUID_TO_SHORT_PREFIX + uuid);
            if (cachedShortId != null) {
                log.debug("✅ [ID-MAPPING] 缓存命中: '{}' -> '{}'", uuid, cachedShortId);
                return Optional.of(cachedShortId);
            }
        } catch (Exception e) {
            log.warn("⚠️ [ID-MAPPING] Redis缓存查询失败: {}", e.getMessage());
        }
        
        // 2. 查数据库
        try {
            Optional<String> dbShortId = mappingRepository.findShortIdByUuid(uuid);
            if (dbShortId.isPresent()) {
                log.debug("💾 [ID-MAPPING] 数据库查询成功: '{}' -> '{}'", uuid, dbShortId.get());
                
                // 3. 回写缓存
                try {
                    redisTemplate.opsForValue().set(UUID_TO_SHORT_PREFIX + uuid, dbShortId.get(), 
                            CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                    redisTemplate.opsForValue().set(SHORT_TO_UUID_PREFIX + dbShortId.get(), uuid, 
                            CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                } catch (Exception e) {
                    log.warn("⚠️ [ID-MAPPING] 缓存回写失败: {}", e.getMessage());
                }
                
                return dbShortId;
            }
        } catch (Exception e) {
            log.error("❌ [ID-MAPPING] 数据库查询失败: uuid='{}', error={}", uuid, e.getMessage(), e);
        }
        
        log.debug("❌ [ID-MAPPING] 未找到映射: uuid='{}'", uuid);
        return Optional.empty();
    }

    @Override
    @Transactional
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
            // 1. 检查是否已存在
            if (mappingRepository.existsByShortIdAndEnabledTrue(shortId)) {
                log.warn("⚠️ [ID-MAPPING] 短格式ID已存在: '{}'", shortId);
                return false;
            }
            
            if (mappingRepository.existsByUuidIdAndEnabledTrue(uuid)) {
                log.warn("⚠️ [ID-MAPPING] UUID已存在: '{}'", uuid);
                return false;
            }
            
            // 2. 创建数据库记录
            WorkspaceIdMapping mapping = WorkspaceIdMapping.builder()
                    .shortId(shortId)
                    .uuidId(uuid)
                    .description("AFFiNE工作空间ID映射")
                    .enabled(true)
                    .build();
            
            mappingRepository.save(mapping);
            log.info("💾 [ID-MAPPING] 数据库映射创建成功: '{}' <-> '{}'", shortId, uuid);
            
            // 3. 创建缓存
            try {
                redisTemplate.opsForValue().set(SHORT_TO_UUID_PREFIX + shortId, uuid, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                redisTemplate.opsForValue().set(UUID_TO_SHORT_PREFIX + uuid, shortId, CACHE_EXPIRE_DAYS, TimeUnit.DAYS);
                log.debug("📝 [ID-MAPPING] 缓存创建成功");
            } catch (Exception e) {
                log.warn("⚠️ [ID-MAPPING] 缓存创建失败: {}", e.getMessage());
                // 缓存失败不影响整体功能
            }
            
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
    
    /**
     * 自动创建缺失的映射关系
     * 当发现数据库中存在某个工作空间，但缺少对应的ID映射时使用此方法
     */
    @Transactional
    public boolean autoCreateMappingIfMissing(String workspaceId, String alternativeId) {
        if (workspaceId == null || alternativeId == null) {
            return false;
        }
        
        String shortId = null;
        String uuid = null;
        
        // 识别哪个是短格式，哪个是UUID格式
        if (isShortFormat(workspaceId) && isUuidFormat(alternativeId)) {
            shortId = workspaceId;
            uuid = alternativeId;
        } else if (isUuidFormat(workspaceId) && isShortFormat(alternativeId)) {
            shortId = alternativeId;
            uuid = workspaceId;
        } else {
            log.debug("🤷 [ID-MAPPING] 无法自动创建映射，ID格式不匹配: '{}' <-> '{}'", 
                    workspaceId, alternativeId);
            return false;
        }
        
        // 检查映射是否已存在
        if (shortIdToUuid(shortId).isPresent()) {
            log.debug("✅ [ID-MAPPING] 映射已存在，无需创建: '{}' <-> '{}'", shortId, uuid);
            return true;
        }
        
        // 自动创建映射
        log.info("🤖 [ID-MAPPING] 自动创建缺失的映射: '{}' <-> '{}'", shortId, uuid);
        return createMapping(shortId, uuid);
    }
}