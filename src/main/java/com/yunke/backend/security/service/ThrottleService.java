package com.yunke.backend.security.service;

import com.yunke.backend.common.dto.PaginatedResponse;
import com.yunke.backend.security.dto.throttle.ThrottleConfigDto;
import com.yunke.backend.security.dto.throttle.ThrottleConfigDto.Throttlers;
import com.yunke.backend.security.dto.throttle.ThrottleLogEntryDto;
import com.yunke.backend.security.dto.throttle.ThrottlePresetDto;
import com.yunke.backend.security.dto.throttle.ThrottleStatsDto;
import com.yunke.backend.security.dto.throttle.ThrottleTestResultDto;
import com.yunke.backend.security.dto.throttle.ThrottleValidationResultDto;
import com.yunke.backend.security.dto.throttle.ThrottlerConfigDto;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * 提供访问限流相关的模拟数据与状态管理，确保前端在缺少真实限流实现时也能正常运行。
 */
@Service
@Slf4j
public class ThrottleService {

    private static final List<String> SUPPORTED_LEVELS = Arrays.asList("INFO", "WARN", "ERROR");

    private final AtomicReference<ThrottleConfigDto> configRef = new AtomicReference<>(createDefaultConfig());
    private final List<ThrottlePresetDto> presetList = createPresets();
    private final AtomicLong totalRequestCounter = new AtomicLong(12_480);
    private final AtomicLong blockedRequestCounter = new AtomicLong(132);

    public ThrottleConfigDto getCurrentConfig() {
        return deepCopy(configRef.get());
    }

    public ThrottleConfigDto updateConfig(ThrottleConfigDto newConfig) {
        ThrottleValidationResultDto validation = validateConfig(newConfig);
        if (!Boolean.TRUE.equals(validation.getValid())) {
            throw new IllegalArgumentException(String.join("; ", validation.getErrors()));
        }

        ThrottleConfigDto sanitized = sanitize(newConfig);
        configRef.set(sanitized);
        log.info("Throttle configuration updated: enabled={}, defaultLimit={}, strictLimit={}",
                sanitized.getEnabled(),
                Optional.ofNullable(sanitized.getThrottlers())
                        .map(Throttlers::getDefaultConfig)
                        .map(ThrottlerConfigDto::getLimit)
                        .orElse(null),
                Optional.ofNullable(sanitized.getThrottlers())
                        .map(Throttlers::getStrict)
                        .map(ThrottlerConfigDto::getLimit)
                        .orElse(null));
        return deepCopy(sanitized);
    }

    public ThrottleValidationResultDto validateConfig(ThrottleConfigDto config) {
        ThrottleValidationResultDto result = ThrottleValidationResultDto.builder().build();
        ThrottleConfigDto sanitized = sanitize(config);

        validateThrottler("默认限流器", sanitized.getThrottlers().getDefaultConfig(), result);
        validateThrottler("严格限流器", sanitized.getThrottlers().getStrict(), result);

        if (Boolean.FALSE.equals(sanitized.getEnabled())) {
            result.getWarnings().add("限流当前处于关闭状态，请确认是否符合生产要求。");
        }

        result.setValid(result.getErrors().isEmpty());
        return result;
    }

    public ThrottleTestResultDto testConfig(ThrottleConfigDto config) {
        ThrottleConfigDto sanitized = sanitize(config);
        ThrottlerConfigDto defaultConfig = sanitized.getThrottlers().getDefaultConfig();
        int simulatedRequests = defaultConfig.getLimit() != null ? defaultConfig.getLimit() : 60;
        int simulatedBlocked = Math.max(0, simulatedRequests / 10);

        return ThrottleTestResultDto.builder()
                .success(true)
                .message(String.format(Locale.ROOT,
                        "模拟在 %d 秒窗口内发送 %d 次请求，预计阻断 %d 次请求。",
                        defaultConfig.getTtl(), simulatedRequests, simulatedBlocked))
                .details("此结果基于当前配置的简单模拟，用于验证阈值是否符合预期。")
                .testRequests(simulatedRequests)
                .blockedRequests(simulatedBlocked)
                .build();
    }

    public ThrottleStatsDto getStats() {
        ThrottleConfigDto current = configRef.get();
        long additionalRequests = ThreadLocalRandom.current().nextLong(25, 90);
        long additionalBlocked = ThreadLocalRandom.current().nextLong(0, 6);

        long total = totalRequestCounter.addAndGet(additionalRequests);
        long blocked = blockedRequestCounter.addAndGet(additionalBlocked);
        int rpm = (int) Math.max(40, additionalRequests * 2 + ThreadLocalRandom.current().nextInt(80, 160));

        return ThrottleStatsDto.builder()
                .enabled(current.getEnabled())
                .activeThrottlers(Boolean.TRUE.equals(current.getEnabled()) ? 2 : 0)
                .totalRequests(total)
                .blockedRequests(blocked)
                .requestsPerMinute(rpm)
                .build();
    }

    public List<ThrottlePresetDto> getPresets() {
        return presetList;
    }

    public Optional<ThrottlePresetDto> getPreset(String id) {
        return presetList.stream().filter(preset -> preset.getId().equalsIgnoreCase(id)).findFirst();
    }

    public void reload() {
        // 在真实实现中此处会刷新限流配置，本地模拟直接重置统计数据的抖动。
        totalRequestCounter.addAndGet(ThreadLocalRandom.current().nextLong(10, 40));
        blockedRequestCounter.addAndGet(ThreadLocalRandom.current().nextLong(0, 4));
        log.info("Throttle configuration reloaded (simulated).");
    }

    public PaginatedResponse<ThrottleLogEntryDto> getLogs(int page, int size, String level) {
        int pageIndex = Math.max(page - 1, 0);
        int pageSize = Math.min(Math.max(size, 1), 100);
        String normalizedLevel = StringUtils.hasText(level) ? level.toUpperCase(Locale.ROOT) : "ALL";

        List<ThrottleLogEntryDto> entries = new ArrayList<>();
        for (int i = 0; i < pageSize; i++) {
            Instant timestamp = Instant.now().minusSeconds((long) (pageIndex * pageSize + i) * 45);
            String computedLevel = SUPPORTED_LEVELS.get(i % SUPPORTED_LEVELS.size());
            if (!"ALL".equals(normalizedLevel) && !computedLevel.equalsIgnoreCase(normalizedLevel)) {
                continue;
            }
            entries.add(ThrottleLogEntryDto.builder()
                    .id("log-" + (pageIndex * pageSize + i + 1))
                    .timestamp(timestamp)
                    .level(computedLevel)
                    .rule(i % 2 == 0 ? "default" : "strict")
                    .clientIp(String.format(Locale.ROOT, "203.0.113.%d", 10 + (i % 50)))
                    .requestPath(i % 2 == 0 ? "/api/documents" : "/api/admin/settings")
                    .requestCount(20 + i)
                    .action(i % 3 == 0 ? "BLOCKED" : "ALLOWED")
                    .message(i % 3 == 0 ? "超过阈值，已触发限流" : "请求计数更新")
                    .build());
        }

        long total = 180L;
        return PaginatedResponse.of(entries, total, pageIndex, pageSize);
    }

    private void validateThrottler(String label, ThrottlerConfigDto config, ThrottleValidationResultDto result) {
        if (config == null) {
            result.getErrors().add(label + " 配置缺失。");
            return;
        }
        if (config.getTtl() == null || config.getTtl() <= 0) {
            result.getErrors().add(label + " 的 TTL 必须大于 0 秒。");
        }
        if (config.getLimit() == null || config.getLimit() <= 0) {
            result.getErrors().add(label + " 的请求上限必须大于 0。");
        }
        if (config.getBlockDuration() != null && config.getBlockDuration() < 0) {
            result.getErrors().add(label + " 的阻断时长不能为负数。");
        }
        if (config.getIgnoreUserAgents().size() > 20) {
            result.getWarnings().add(label + " 忽略的 User-Agent 条目较多，可能影响维护成本。");
        }
    }

    private ThrottleConfigDto sanitize(ThrottleConfigDto config) {
        ThrottleConfigDto current = config != null ? config : createDefaultConfig();
        ThrottleConfigDto.Throttlers throttlers = current.getThrottlers();
        if (throttlers == null) {
            throttlers = ThrottleConfigDto.Throttlers.builder().build();
        }

        ThrottlerConfigDto defaultConfig = throttlers.getDefaultConfig();
        if (defaultConfig == null) {
            defaultConfig = createDefaultThrottler();
        }
        if (defaultConfig.getIgnoreUserAgents() == null) {
            defaultConfig.setIgnoreUserAgents(new ArrayList<>());
        }

        ThrottlerConfigDto strictConfig = throttlers.getStrict();
        if (strictConfig == null) {
            strictConfig = createStrictThrottler();
        }
        if (strictConfig.getIgnoreUserAgents() == null) {
            strictConfig.setIgnoreUserAgents(new ArrayList<>());
        }

        return ThrottleConfigDto.builder()
                .enabled(Boolean.TRUE.equals(current.getEnabled()))
                .throttlers(ThrottleConfigDto.Throttlers.builder()
                        .defaultConfig(defaultConfig)
                        .strict(strictConfig)
                        .build())
                .build();
    }

    private ThrottleConfigDto deepCopy(ThrottleConfigDto source) {
        if (source == null) {
            return createDefaultConfig();
        }
        ThrottleConfigDto.Throttlers throttlers = source.getThrottlers();
        if (throttlers == null) {
            throttlers = ThrottleConfigDto.Throttlers.builder()
                    .defaultConfig(createDefaultThrottler())
                    .strict(createStrictThrottler())
                    .build();
        }
        ThrottlerConfigDto defaultCopy = copyThrottler(throttlers.getDefaultConfig());
        ThrottlerConfigDto strictCopy = copyThrottler(throttlers.getStrict());
        return ThrottleConfigDto.builder()
                .enabled(source.getEnabled())
                .throttlers(ThrottleConfigDto.Throttlers.builder()
                        .defaultConfig(defaultCopy)
                        .strict(strictCopy)
                        .build())
                .build();
    }

    private ThrottlerConfigDto copyThrottler(ThrottlerConfigDto original) {
        if (original == null) {
            return createDefaultThrottler();
        }
        return ThrottlerConfigDto.builder()
                .ttl(original.getTtl())
                .limit(original.getLimit())
                .blockDuration(original.getBlockDuration())
                .skipIf(original.getSkipIf())
                .ignoreUserAgents(original.getIgnoreUserAgents() == null
                        ? new ArrayList<>()
                        : new ArrayList<>(original.getIgnoreUserAgents()))
                .build();
    }

    private ThrottleConfigDto createDefaultConfig() {
        return ThrottleConfigDto.builder()
                .enabled(Boolean.FALSE)
                .throttlers(ThrottleConfigDto.Throttlers.builder()
                        .defaultConfig(createDefaultThrottler())
                        .strict(createStrictThrottler())
                        .build())
                .build();
    }

    private ThrottlerConfigDto createDefaultThrottler() {
        return ThrottlerConfigDto.builder()
                .ttl(60)
                .limit(60)
                .blockDuration(120)
                .ignoreUserAgents(new ArrayList<>())
                .build();
    }

    private ThrottlerConfigDto createStrictThrottler() {
        return ThrottlerConfigDto.builder()
                .ttl(60)
                .limit(30)
                .blockDuration(300)
                .ignoreUserAgents(new ArrayList<>())
                .build();
    }

    private List<ThrottlePresetDto> createPresets() {
        return Collections.unmodifiableList(Arrays.asList(
                ThrottlePresetDto.builder()
                        .id("lenient")
                        .name("宽松模式")
                        .description("适合高流量场景，容忍度较高")
                        .config(ThrottlerConfigDto.builder()
                                .ttl(60)
                                .limit(100)
                                .blockDuration(60)
                                .ignoreUserAgents(new ArrayList<>())
                                .build())
                        .build(),
                ThrottlePresetDto.builder()
                        .id("balanced")
                        .name("平衡模式")
                        .description("兼顾性能与安全的推荐配置")
                        .config(ThrottlerConfigDto.builder()
                                .ttl(60)
                                .limit(60)
                                .blockDuration(180)
                                .ignoreUserAgents(new ArrayList<>())
                                .build())
                        .build(),
                ThrottlePresetDto.builder()
                        .id("strict")
                        .name("严格模式")
                        .description("高安全要求下的严格限制")
                        .config(ThrottlerConfigDto.builder()
                                .ttl(60)
                                .limit(30)
                                .blockDuration(600)
                                .ignoreUserAgents(new ArrayList<>())
                                .build())
                        .build()
        ));
    }
}

