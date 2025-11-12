package com.yunke.backend.workspace.controller;

import com.yunke.backend.workspace.service.WorkspaceIdMappingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 工作空间ID映射管理控制器
 * 用于调试和管理AFFiNE前端的双重ID系统
 */
@RestController
@RequestMapping("/api/debug/workspace-id-mapping")
@RequiredArgsConstructor
@Slf4j
public class WorkspaceIdMappingController {
    
    private final WorkspaceIdMappingService workspaceIdMappingService;
    
    /**
     * 创建新的ID映射关系
     */
    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createMapping(
            @RequestBody CreateMappingRequest request) {
        
        log.info("🔧 [ID-MAPPING-API] 创建映射请求: shortId='{}', uuid='{}'", 
                request.shortId(), request.uuid());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            boolean success = workspaceIdMappingService.createMapping(request.shortId(), request.uuid());
            
            if (success) {
                response.put("success", true);
                response.put("message", "映射创建成功");
                response.put("shortId", request.shortId());
                response.put("uuid", request.uuid());
                
                log.info("✅ [ID-MAPPING-API] 映射创建成功: '{}' <-> '{}'", 
                        request.shortId(), request.uuid());
                
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "映射创建失败");
                
                log.warn("❌ [ID-MAPPING-API] 映射创建失败: '{}' <-> '{}'", 
                        request.shortId(), request.uuid());
                
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("❌ [ID-MAPPING-API] 映射创建异常: shortId='{}', uuid='{}', error={}", 
                    request.shortId(), request.uuid(), e.getMessage(), e);
            
            response.put("success", false);
            response.put("error", "内部服务器错误: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 查询短格式ID对应的UUID
     */
    @GetMapping("/short-to-uuid/{shortId}")
    public ResponseEntity<Map<String, Object>> shortToUuid(@PathVariable String shortId) {
        
        log.debug("🔍 [ID-MAPPING-API] 查询短格式ID映射: shortId='{}'", shortId);
        
        Map<String, Object> response = new HashMap<>();
        response.put("shortId", shortId);
        
        try {
            Optional<String> uuid = workspaceIdMappingService.shortIdToUuid(shortId);
            
            if (uuid.isPresent()) {
                response.put("success", true);
                response.put("uuid", uuid.get());
                response.put("message", "找到映射");
                
                log.debug("✅ [ID-MAPPING-API] 找到映射: '{}' -> '{}'", shortId, uuid.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "未找到映射");
                
                log.debug("❌ [ID-MAPPING-API] 未找到映射: shortId='{}'", shortId);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("❌ [ID-MAPPING-API] 查询映射异常: shortId='{}', error={}", 
                    shortId, e.getMessage(), e);
            
            response.put("success", false);
            response.put("error", "内部服务器错误: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 查询UUID对应的短格式ID
     */
    @GetMapping("/uuid-to-short/{uuid}")
    public ResponseEntity<Map<String, Object>> uuidToShort(@PathVariable String uuid) {
        
        log.debug("🔍 [ID-MAPPING-API] 查询UUID映射: uuid='{}'", uuid);
        
        Map<String, Object> response = new HashMap<>();
        response.put("uuid", uuid);
        
        try {
            Optional<String> shortId = workspaceIdMappingService.uuidToShortId(uuid);
            
            if (shortId.isPresent()) {
                response.put("success", true);
                response.put("shortId", shortId.get());
                response.put("message", "找到映射");
                
                log.debug("✅ [ID-MAPPING-API] 找到映射: '{}' -> '{}'", uuid, shortId.get());
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "未找到映射");
                
                log.debug("❌ [ID-MAPPING-API] 未找到映射: uuid='{}'", uuid);
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("❌ [ID-MAPPING-API] 查询映射异常: uuid='{}', error={}", 
                    uuid, e.getMessage(), e);
            
            response.put("success", false);
            response.put("error", "内部服务器错误: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 智能转换ID
     */
    @GetMapping("/smart-convert/{id}")
    public ResponseEntity<Map<String, Object>> smartConvert(@PathVariable String id) {
        
        log.debug("🧠 [ID-MAPPING-API] 智能转换ID: id='{}'", id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("originalId", id);
        
        try {
            String convertedId = workspaceIdMappingService.smartConvert(id);
            boolean isShortFormat = workspaceIdMappingService.isShortFormat(id);
            boolean isUuidFormat = workspaceIdMappingService.isUuidFormat(id);
            
            response.put("success", true);
            response.put("convertedId", convertedId);
            response.put("isConverted", !convertedId.equals(id));
            response.put("originalFormat", isShortFormat ? "short" : (isUuidFormat ? "uuid" : "unknown"));
            response.put("convertedFormat", workspaceIdMappingService.isShortFormat(convertedId) ? "short" : 
                    (workspaceIdMappingService.isUuidFormat(convertedId) ? "uuid" : "unknown"));
            
            log.debug("🔄 [ID-MAPPING-API] 智能转换完成: '{}' -> '{}' (converted={})", 
                    id, convertedId, !convertedId.equals(id));
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [ID-MAPPING-API] 智能转换异常: id='{}', error={}", 
                    id, e.getMessage(), e);
            
            response.put("success", false);
            response.put("error", "内部服务器错误: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取真实的工作空间ID（优先返回UUID格式）
     */
    @GetMapping("/real-workspace-id/{id}")
    public ResponseEntity<Map<String, Object>> getRealWorkspaceId(@PathVariable String id) {
        
        log.debug("🎯 [ID-MAPPING-API] 获取真实工作空间ID: id='{}'", id);
        
        Map<String, Object> response = new HashMap<>();
        response.put("inputId", id);
        
        try {
            String realId = workspaceIdMappingService.getRealWorkspaceId(id);
            boolean isConverted = !realId.equals(id);
            
            response.put("success", true);
            response.put("realWorkspaceId", realId);
            response.put("isConverted", isConverted);
            response.put("inputFormat", workspaceIdMappingService.isShortFormat(id) ? "short" : 
                    (workspaceIdMappingService.isUuidFormat(id) ? "uuid" : "unknown"));
            response.put("outputFormat", workspaceIdMappingService.isShortFormat(realId) ? "short" : 
                    (workspaceIdMappingService.isUuidFormat(realId) ? "uuid" : "unknown"));
            
            log.debug("🎯 [ID-MAPPING-API] 真实工作空间ID: '{}' -> '{}' (converted={})", 
                    id, realId, isConverted);
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ [ID-MAPPING-API] 获取真实工作空间ID异常: id='{}', error={}", 
                    id, e.getMessage(), e);
            
            response.put("success", false);
            response.put("error", "内部服务器错误: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 根据日志中的两个ID自动创建映射
     */
    @PostMapping("/auto-create-from-log")
    public ResponseEntity<Map<String, Object>> autoCreateFromLog(
            @RequestBody AutoCreateMappingRequest request) {
        
        log.info("🤖 [ID-MAPPING-API] 从日志自动创建映射: id1='{}', id2='{}'", 
                request.id1(), request.id2());
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String shortId = null;
            String uuid = null;
            
            // 自动识别哪个是短格式，哪个是UUID格式
            if (workspaceIdMappingService.isShortFormat(request.id1()) && 
                workspaceIdMappingService.isUuidFormat(request.id2())) {
                shortId = request.id1();
                uuid = request.id2();
            } else if (workspaceIdMappingService.isUuidFormat(request.id1()) && 
                       workspaceIdMappingService.isShortFormat(request.id2())) {
                shortId = request.id2();
                uuid = request.id1();
            } else {
                response.put("success", false);
                response.put("error", "无法识别ID格式，需要一个短格式ID和一个UUID格式ID");
                response.put("id1Format", getIdFormat(request.id1()));
                response.put("id2Format", getIdFormat(request.id2()));
                
                log.warn("❌ [ID-MAPPING-API] ID格式不匹配: id1='{}' ({}), id2='{}' ({})", 
                        request.id1(), getIdFormat(request.id1()), 
                        request.id2(), getIdFormat(request.id2()));
                
                return ResponseEntity.badRequest().body(response);
            }
            
            // 创建映射
            boolean success = workspaceIdMappingService.createMapping(shortId, uuid);
            
            if (success) {
                response.put("success", true);
                response.put("message", "自动映射创建成功");
                response.put("shortId", shortId);
                response.put("uuid", uuid);
                
                log.info("✅ [ID-MAPPING-API] 自动映射创建成功: '{}' <-> '{}'", shortId, uuid);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("error", "映射创建失败");
                
                log.warn("❌ [ID-MAPPING-API] 自动映射创建失败: '{}' <-> '{}'", shortId, uuid);
                return ResponseEntity.badRequest().body(response);
            }
        } catch (Exception e) {
            log.error("❌ [ID-MAPPING-API] 自动创建映射异常: id1='{}', id2='{}', error={}", 
                    request.id1(), request.id2(), e.getMessage(), e);
            
            response.put("success", false);
            response.put("error", "内部服务器错误: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }
    
    /**
     * 获取ID格式字符串描述
     */
    private String getIdFormat(String id) {
        if (workspaceIdMappingService.isShortFormat(id)) {
            return "short";
        } else if (workspaceIdMappingService.isUuidFormat(id)) {
            return "uuid";
        } else {
            return "unknown";
        }
    }
    
    // 请求数据类
    public record CreateMappingRequest(String shortId, String uuid) {}
    public record AutoCreateMappingRequest(String id1, String id2) {}
}