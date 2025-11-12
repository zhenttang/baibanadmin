package com.yunke.backend.forum.service;

import com.yunke.backend.forum.dto.*;
import com.yunke.backend.forum.domain.entity.Forum;
import com.yunke.backend.forum.domain.entity.ForumModerator;
import com.yunke.backend.forum.repository.ForumModeratorRepository;
import com.yunke.backend.forum.repository.ForumRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ModeratorService {

    private final ForumModeratorRepository forumModeratorRepository;
    private final ForumRepository forumRepository;
    private final ObjectMapper objectMapper;

    @Transactional(rollbackFor = Exception.class)
    public ModeratorDTO addModerator(CreateModeratorRequest request) {
        if (request == null || request.getForumId() == null) {
            throw new IllegalArgumentException("forumId不能为空");
        }
        if (request.getUserId() == null || request.getUserId().isBlank()) {
            throw new IllegalArgumentException("userId不能为空");
        }

        // Ensure forum exists
        Forum forum = forumRepository.findById(request.getForumId())
                .orElseThrow(() -> new IllegalArgumentException("板块不存在"));

        // Check duplication
        if (forumModeratorRepository.existsByForumIdAndUserId(request.getForumId(), request.getUserId())) {
            throw new IllegalArgumentException("该用户已是该板块版主");
        }

        ForumModerator moderator = new ForumModerator();
        moderator.setForumId(request.getForumId());
        moderator.setUserId(request.getUserId());
        moderator.setUserName(request.getUserName());

        // Parse role with fallback
        try {
            ForumModerator.ModeratorRole role = Optional.ofNullable(request.getRole())
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(String::toUpperCase)
                    .map(ForumModerator.ModeratorRole::valueOf)
                    .orElse(ForumModerator.ModeratorRole.DEPUTY);
            moderator.setRole(role);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("无效的版主角色: " + request.getRole());
        }

        // default permissions: if null/blank/empty object/array, set by role
        String reqPerms = Optional.ofNullable(request.getPermissions()).map(String::trim).orElse("");
        if (reqPerms.isEmpty() || "{}".equals(reqPerms) || "[]".equals(reqPerms)) {
            moderator.setPermissions(defaultPermissionsJson(moderator.getRole()));
        } else {
            moderator.setPermissions(reqPerms);
        }

        ForumModerator saved = forumModeratorRepository.save(moderator);
        return toDTO(saved, forum.getName());
    }

    @Transactional(readOnly = true)
    public List<ModeratorDTO> listModerators(Long forumId) {
        if (forumId == null) {
            throw new IllegalArgumentException("forumId不能为空");
        }

        Forum forum = forumRepository.findById(forumId)
                .orElseThrow(() -> new IllegalArgumentException("板块不存在"));

        return forumModeratorRepository.findByForumId(forumId).stream()
                .map(m -> toDTO(m, forum.getName()))
                .collect(Collectors.toList());
    }

    @Transactional(rollbackFor = Exception.class)
    public void removeModerator(Long forumId, String userId) {
        if (forumId == null) {
            throw new IllegalArgumentException("forumId不能为空");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId不能为空");
        }

        boolean exists = forumModeratorRepository.existsByForumIdAndUserId(forumId, userId);
        if (!exists) {
            throw new IllegalArgumentException("版主不存在");
        }
        forumModeratorRepository.deleteByForumIdAndUserId(forumId, userId);
    }

    // 删除版主（按主键ID）
    @Transactional(rollbackFor = Exception.class)
    public Boolean removeModerator(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id不能为空");
        }
        boolean exists = forumModeratorRepository.existsById(id);
        if (!exists) {
            throw new IllegalArgumentException("版主不存在");
        }
        forumModeratorRepository.deleteById(id);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public ModeratorDTO updatePermissions(Long forumId, String userId, String permissions) {
        if (forumId == null) {
            throw new IllegalArgumentException("forumId不能为空");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId不能为空");
        }
        if (permissions == null) {
            throw new IllegalArgumentException("permissions不能为空");
        }

        ForumModerator moderator = forumModeratorRepository.findByForumIdAndUserId(forumId, userId)
                .orElseThrow(() -> new IllegalArgumentException("版主不存在"));

        moderator.setPermissions(permissions);
        ForumModerator saved = forumModeratorRepository.save(moderator);

        String forumName = forumRepository.findById(forumId)
                .map(Forum::getName)
                .orElse(null);

        return toDTO(saved, forumName);
    }

    // 更新版主权限（按主键ID + Map入参）
    @Transactional(rollbackFor = Exception.class)
    public ModeratorDTO updatePermissions(Long id, java.util.Map<String, Object> permissions) {
        if (id == null) {
            throw new IllegalArgumentException("id不能为空");
        }
        if (permissions == null) {
            throw new IllegalArgumentException("permissions不能为空");
        }

        ForumModerator moderator = forumModeratorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("版主不存在"));

        String json;
        try {
            json = objectMapper.writeValueAsString(permissions);
        } catch (Exception e) {
            throw new IllegalArgumentException("权限格式不正确");
        }

        moderator.setPermissions(json);
        ForumModerator saved = forumModeratorRepository.save(moderator);

        String forumName = forumRepository.findById(saved.getForumId())
                .map(Forum::getName)
                .orElse(null);
        return toDTO(saved, forumName);
    }

    // 更新版主权限（按主键ID + 列表入参）
    @Transactional(rollbackFor = Exception.class)
    public ModeratorDTO updatePermissions(Long id, UpdatePermissionsRequest request) {
        if (id == null) {
            throw new IllegalArgumentException("id不能为空");
        }
        if (request == null || request.getPermissions() == null) {
            throw new IllegalArgumentException("permissions不能为空");
        }

        ForumModerator moderator = forumModeratorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("版主不存在"));

        String json;
        try {
            json = objectMapper.writeValueAsString(request.getPermissions()); // JSON数组形式
        } catch (Exception e) {
            throw new IllegalArgumentException("权限格式不正确");
        }

        moderator.setPermissions(json);
        ForumModerator saved = forumModeratorRepository.save(moderator);

        String forumName = forumRepository.findById(saved.getForumId())
                .map(Forum::getName)
                .orElse(null);
        return toDTO(saved, forumName);
    }

    public ModeratorDTO toDTO(ForumModerator entity) {
        String forumName = forumRepository.findById(entity.getForumId())
                .map(Forum::getName)
                .orElse(null);
        return toDTO(entity, forumName);
    }

    private ModeratorDTO toDTO(ForumModerator entity, String forumName) {
        ModeratorDTO dto = new ModeratorDTO();
        dto.setId(entity.getId());
        dto.setForumId(entity.getForumId());
        dto.setForumName(forumName);
        dto.setUserId(entity.getUserId());
        dto.setUserName(entity.getUserName());
        dto.setRole(entity.getRole() != null ? entity.getRole().name() : null);
        dto.setPermissions(entity.getPermissions());
        dto.setAppointedBy(entity.getAppointedBy());
        dto.setAppointedAt(entity.getAppointedAt());
        dto.setExpiresAt(entity.getExpiresAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }

    // 权限判断
    @Transactional(readOnly = true)
    public Boolean hasPermission(Long forumId, String userId, String permission) {
        if (forumId == null || userId == null || userId.isBlank() || permission == null || permission.isBlank()) {
            return false;
        }
        return forumModeratorRepository.findByForumIdAndUserId(forumId, userId)
                .map(m -> hasPermissionInternal(m.getPermissions(), permission))
                .orElse(false);
    }

    private boolean hasPermissionInternal(String permissionsJson, String permission) {
        if (permissionsJson == null || permissionsJson.isBlank()) {
            return false;
        }
        String trimmed = permissionsJson.trim();
        try {
            if (trimmed.startsWith("{")) {
                java.util.Map<String, Object> map = objectMapper.readValue(trimmed, new TypeReference<java.util.Map<String, Object>>(){});
                Object val = map.get(permission);
                if (val instanceof Boolean) return (Boolean) val;
                if (val instanceof Number) return ((Number) val).intValue() != 0;
                if (val instanceof String) return Boolean.parseBoolean((String) val);
                return false;
            } else if (trimmed.startsWith("[")) {
                java.util.List<String> list = objectMapper.readValue(trimmed, new TypeReference<java.util.List<String>>(){});
                return list.stream().anyMatch(p -> p != null && p.equalsIgnoreCase(permission));
            } else {
                // fallback comma-delimited
                for (String p : trimmed.split(",")) {
                    if (permission.equalsIgnoreCase(p.trim())) return true;
                }
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    // 默认权限：根据角色生成JSON对象字符串
    private String defaultPermissionsJson(ForumModerator.ModeratorRole role) {
        java.util.Map<String, Object> map = new java.util.LinkedHashMap<>();
        if (role == ForumModerator.ModeratorRole.CHIEF) {
            map.put("PIN", true);
            map.put("FEATURE", true);
            map.put("LOCK", true);
            map.put("DELETE", true);
            map.put("EDIT", true);
        } else { // DEPUTY and others default
            map.put("PIN", true);
            map.put("LOCK", true);
        }
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }
}
