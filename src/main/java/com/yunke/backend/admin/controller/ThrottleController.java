package com.yunke.backend.admin.controller;

import com.yunke.backend.common.dto.PaginatedResponse;
import com.yunke.backend.security.dto.throttle.ThrottleConfigDto;
import com.yunke.backend.security.dto.throttle.ThrottleLogEntryDto;
import com.yunke.backend.security.dto.throttle.ThrottlePresetDto;
import com.yunke.backend.security.dto.throttle.ThrottleStatsDto;
import com.yunke.backend.security.dto.throttle.ThrottleTestResultDto;
import com.yunke.backend.security.dto.throttle.ThrottleValidationResultDto;
import com.yunke.backend.security.service.ThrottleService;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * 管理端访问限流相关接口。
 */
@RestController
@RequestMapping("/api/admin/throttle")
@RequiredArgsConstructor
@Slf4j
@PreAuthorize("hasRole('ADMIN')")
public class ThrottleController {

    private final ThrottleService throttleService;

    @GetMapping("/config")
    public ThrottleConfigDto getConfig() {
        return throttleService.getCurrentConfig();
    }

    @PutMapping("/config")
    public ThrottleConfigDto updateConfig(@RequestBody ThrottleConfigDto config) {
        try {
            return throttleService.updateConfig(config);
        } catch (IllegalArgumentException ex) {
            log.warn("Failed to update throttle config: {}", ex.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        }
    }

    @PostMapping("/validate")
    public ThrottleValidationResultDto validateConfig(@RequestBody ThrottleConfigDto config) {
        return throttleService.validateConfig(config);
    }

    @PostMapping("/test")
    public ThrottleTestResultDto testThrottle(@RequestBody ThrottleConfigDto config) {
        return throttleService.testConfig(config);
    }

    @PostMapping("/reload")
    public Map<String, Object> reload() {
        throttleService.reload();
        Map<String, Object> response = new HashMap<>();
        response.put("success", Boolean.TRUE);
        response.put("message", "Throttle configuration reloaded.");
        return response;
    }

    @GetMapping("/stats")
    public ThrottleStatsDto getStats() {
        return throttleService.getStats();
    }

    @GetMapping("/presets")
    public List<ThrottlePresetDto> getPresets() {
        return throttleService.getPresets();
    }

    @GetMapping("/presets/{presetId}")
    public ResponseEntity<ThrottlePresetDto> getPreset(@PathVariable String presetId) {
        Optional<ThrottlePresetDto> preset = throttleService.getPreset(presetId);
        return preset.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @GetMapping("/logs")
    public PaginatedResponse<ThrottleLogEntryDto> getLogs(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String level) {
        String normalizedLevel = level != null ? level.toUpperCase(Locale.ROOT) : null;
        return throttleService.getLogs(page, size, normalizedLevel);
    }
}
