package com.yunke.backend.security.service.impl;

import com.yunke.backend.security.dto.SecurityConfigDto;
import com.yunke.backend.security.dto.SecurityEventDto;
import com.yunke.backend.security.dto.SecurityReportDto;
import com.yunke.backend.system.domain.entity.IpAccessControl;
import com.yunke.backend.system.domain.entity.SecurityEvent;
import com.yunke.backend.security.repository.IpAccessControlRepository;
import com.yunke.backend.security.repository.SecurityEventRepository;
import com.yunke.backend.system.service.ConfigService;
import com.yunke.backend.system.service.ConfigLogService;
import com.yunke.backend.security.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 安全管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SecurityServiceImpl implements SecurityService {
    
    private final ConfigService configService;
    private final ConfigLogService configLogService;
    private final SecurityEventRepository securityEventRepository;
    private final IpAccessControlRepository ipAccessControlRepository;
    
    private static final String CONFIG_MODULE = "security";
    
    // 常见密码模式
    private static final List<Pattern> WEAK_PATTERNS = Arrays.asList(
            Pattern.compile("^(\\d)\\1{2,}$"), // 重复数字
            Pattern.compile("^(.)\\1{2,}$"), // 重复字符
            Pattern.compile("^(\\d{3,})$"), // 纯数字
            Pattern.compile("^([a-zA-Z]{3,})$"), // 纯字母
            Pattern.compile("^(19|20)\\d{2}$"), // 年份
            Pattern.compile("^\\d{2}[/-]\\d{2}[/-]\\d{2,4}$") // 日期格式
    );
    
    // 常见弱密码
    private static final Set<String> COMMON_PASSWORDS = Set.of(
            "password", "123456", "123456789", "qwerty", "abc123",
            "password123", "admin", "root", "user", "guest"
    );
    
    @Override
    public SecurityConfigDto getSecurityConfig() {
        try {
            Map<String, Object> configMap = configService.getModuleConfig(CONFIG_MODULE);
            
            SecurityConfigDto config = new SecurityConfigDto();
            
            if (configMap != null) {
                config.setEnabled((Boolean) configMap.getOrDefault("enabled", true));
                
                // 登录安全配置
                Map<String, Object> loginSecurity = (Map<String, Object>) configMap.get("loginSecurity");
                if (loginSecurity != null) {
                    config.setLoginSecurity(SecurityConfigDto.LoginSecurityConfig.builder()
                            .maxFailedAttempts((Integer) loginSecurity.getOrDefault("maxFailedAttempts", 5))
                            .lockoutDurationMinutes((Integer) loginSecurity.getOrDefault("lockoutDurationMinutes", 30))
                            .enableCaptcha((Boolean) loginSecurity.getOrDefault("enableCaptcha", true))
                            .captchaThreshold((Integer) loginSecurity.getOrDefault("captchaThreshold", 3))
                            .enableTwoFactor((Boolean) loginSecurity.getOrDefault("enableTwoFactor", false))
                            .enableIpRestriction((Boolean) loginSecurity.getOrDefault("enableIpRestriction", false))
                            .forceSingleSession((Boolean) loginSecurity.getOrDefault("forceSingleSession", false))
                            .build());
                }
                
                // IP访问控制配置
                Map<String, Object> ipAccess = (Map<String, Object>) configMap.get("ipAccess");
                if (ipAccess != null) {
                    config.setIpAccess(SecurityConfigDto.IpAccessConfig.builder()
                            .enableWhitelist((Boolean) ipAccess.getOrDefault("enableWhitelist", false))
                            .whitelist((List<String>) ipAccess.getOrDefault("whitelist", new ArrayList<>()))
                            .enableBlacklist((Boolean) ipAccess.getOrDefault("enableBlacklist", false))
                            .blacklist((List<String>) ipAccess.getOrDefault("blacklist", new ArrayList<>()))
                            .allowPrivateNetworks((Boolean) ipAccess.getOrDefault("allowPrivateNetworks", true))
                            .allowedCountries((List<String>) ipAccess.getOrDefault("allowedCountries", new ArrayList<>()))
                            .build());
                }
                
                // 其他配置...
            }
            
            // 设置默认值
            setDefaultSecurityConfig(config);
            
            return config;
            
        } catch (Exception e) {
            log.error("获取安全配置失败", e);
            return getDefaultSecurityConfig();
        }
    }
    
    @Override
    @Transactional
    public SecurityConfigDto updateSecurityConfig(SecurityConfigDto config, String operator, String sourceIp) {
        try {
            if (!validateSecurityConfig(config)) {
                configLogService.logFailedOperation("UPDATE", CONFIG_MODULE, "security-config",
                        operator, sourceIp, "安全配置验证失败");
                throw new IllegalArgumentException("安全配置验证失败");
            }
            
            SecurityConfigDto oldConfig = getSecurityConfig();
            
            // 构建配置Map
            Map<String, Object> configMap = buildConfigMap(config);
            
            // 保存配置
            configService.updateModuleConfig(CONFIG_MODULE, configMap);
            
            // 记录操作日志
            configLogService.logOperation("UPDATE", CONFIG_MODULE, "security-config",
                    oldConfig.toString(), config.toString(), operator, sourceIp, "更新安全配置");
            
            log.info("安全配置已更新，操作用户: {}", operator);
            
            return getSecurityConfig();
            
        } catch (Exception e) {
            log.error("更新安全配置失败", e);
            configLogService.logFailedOperation("UPDATE", CONFIG_MODULE, "security-config",
                    operator, sourceIp, e.getMessage());
            throw new RuntimeException("更新安全配置失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean validateSecurityConfig(SecurityConfigDto config) {
        try {
            if (config == null) {
                return false;
            }
            
            // 验证登录安全配置
            if (config.getLoginSecurity() != null) {
                SecurityConfigDto.LoginSecurityConfig loginSecurity = config.getLoginSecurity();
                if (loginSecurity.getMaxFailedAttempts() != null && 
                    (loginSecurity.getMaxFailedAttempts() < 1 || loginSecurity.getMaxFailedAttempts() > 20)) {
                    log.warn("最大失败尝试次数超出范围: {}", loginSecurity.getMaxFailedAttempts());
                    return false;
                }
                
                if (loginSecurity.getLockoutDurationMinutes() != null && 
                    loginSecurity.getLockoutDurationMinutes() < 1) {
                    log.warn("锁定时间不能小于1分钟: {}", loginSecurity.getLockoutDurationMinutes());
                    return false;
                }
            }
            
            // 验证密码策略配置
            if (config.getPasswordPolicy() != null) {
                SecurityConfigDto.PasswordPolicyConfig passwordPolicy = config.getPasswordPolicy();
                if (passwordPolicy.getMinLength() != null && passwordPolicy.getMinLength() < 6) {
                    log.warn("密码最小长度不能小于6: {}", passwordPolicy.getMinLength());
                    return false;
                }
                
                if (passwordPolicy.getMaxLength() != null && passwordPolicy.getMaxLength() > 128) {
                    log.warn("密码最大长度不能大于128: {}", passwordPolicy.getMaxLength());
                    return false;
                }
            }
            
            return true;
            
        } catch (Exception e) {
            log.error("安全配置验证过程中发生错误", e);
            return false;
        }
    }
    
    @Override
    @Transactional
    public void recordSecurityEvent(String eventType, String severity, String description,
                                  String userId, String username, String sourceIp,
                                  String userAgent, String requestPath, String requestMethod,
                                  String details) {
        try {
            SecurityEvent event = SecurityEvent.builder()
                    .eventType(eventType)
                    .severity(severity)
                    .description(description)
                    .userId(userId)
                    .username(username)
                    .sourceIp(sourceIp)
                    .userAgent(userAgent)
                    .requestPath(requestPath)
                    .requestMethod(requestMethod)
                    .details(details)
                    .build();
            
            // 尝试获取地理位置信息（简化实现）
            enrichGeoLocation(event, sourceIp);
            
            securityEventRepository.save(event);
            
            log.info("安全事件已记录: {}|{}|{}|{}", eventType, severity, sourceIp, username);
            
            // 检查是否需要实时告警
            checkRealTimeAlert(event);
            
        } catch (Exception e) {
            log.error("记录安全事件失败", e);
        }
    }
    
    @Override
    public Page<SecurityEventDto> getSecurityEvents(String eventType, String severity, Boolean handled,
                                                  String sourceIp, LocalDateTime startTime, LocalDateTime endTime,
                                                  Pageable pageable) {
        try {
            if (startTime == null) {
                startTime = LocalDateTime.now().minusDays(30);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            
            Page<SecurityEvent> events = securityEventRepository.findByFilters(
                    eventType, severity, handled, sourceIp, startTime, endTime, pageable);
            
            return events.map(this::convertToDto);
            
        } catch (Exception e) {
            log.error("获取安全事件失败", e);
            return Page.empty();
        }
    }
    
    @Override
    @Transactional
    public boolean handleSecurityEvent(Long eventId, String resolution, String operator) {
        try {
            Optional<SecurityEvent> eventOpt = securityEventRepository.findById(eventId);
            if (eventOpt.isPresent()) {
                SecurityEvent event = eventOpt.get();
                event.setHandled(true);
                event.setResolution(resolution);
                event.setResolvedAt(LocalDateTime.now());
                event.setResolvedBy(operator);
                
                securityEventRepository.save(event);
                
                log.info("安全事件已处理: {} by {}", eventId, operator);
                return true;
            }
            return false;
            
        } catch (Exception e) {
            log.error("处理安全事件失败", e);
            return false;
        }
    }
    
    @Override
    public boolean isIpAllowed(String ipAddress) {
        try {
            LocalDateTime now = LocalDateTime.now();
            
            // 检查黑名单
            List<IpAccessControl> blacklist = ipAccessControlRepository.findActiveBlacklist(now);
            for (IpAccessControl rule : blacklist) {
                if (isIpMatching(ipAddress, rule.getIpAddress())) {
                    // 更新命中统计
                    ipAccessControlRepository.updateHitCount(rule.getId(), now);
                    log.warn("IP {} 在黑名单中，访问被拒绝", ipAddress);
                    return false;
                }
            }
            
            // 检查白名单
            List<IpAccessControl> whitelist = ipAccessControlRepository.findActiveWhitelist(now);
            if (!whitelist.isEmpty()) {
                for (IpAccessControl rule : whitelist) {
                    if (isIpMatching(ipAddress, rule.getIpAddress())) {
                        // 更新命中统计
                        ipAccessControlRepository.updateHitCount(rule.getId(), now);
                        return true;
                    }
                }
                // 白名单启用但IP不在白名单中
                log.warn("IP {} 不在白名单中，访问被拒绝", ipAddress);
                return false;
            }
            
            // 默认允许
            return true;
            
        } catch (Exception e) {
            log.error("检查IP访问权限失败", e);
            // 发生错误时默认允许访问，避免误封
            return true;
        }
    }
    
    @Override
    @Transactional
    public IpAccessControl addIpAccessRule(String ipAddress, String accessType, String description,
                                         LocalDateTime expiresAt, String operator) {
        try {
            // 检查IP地址是否已存在
            Optional<IpAccessControl> existing = ipAccessControlRepository.findByIpAddress(ipAddress);
            if (existing.isPresent()) {
                throw new IllegalArgumentException("IP地址 " + ipAddress + " 已存在访问控制规则");
            }
            
            IpAccessControl rule = IpAccessControl.builder()
                    .ipAddress(ipAddress)
                    .accessType(accessType)
                    .description(description)
                    .expiresAt(expiresAt)
                    .createdBy(operator)
                    .build();
            
            rule = ipAccessControlRepository.save(rule);
            
            log.info("添加IP访问控制规则: {} {} by {}", accessType, ipAddress, operator);
            
            // 记录安全事件
            recordSecurityEvent("IP_ACCESS_RULE_ADDED", "INFO", 
                    "添加IP访问控制规则: " + accessType + " " + ipAddress,
                    null, operator, null, null, null, null, 
                    "rule_id=" + rule.getId());
            
            return rule;
            
        } catch (Exception e) {
            log.error("添加IP访问控制规则失败", e);
            throw new RuntimeException("添加IP访问控制规则失败: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional
    public boolean removeIpAccessRule(Long ruleId, String operator) {
        try {
            Optional<IpAccessControl> ruleOpt = ipAccessControlRepository.findById(ruleId);
            if (ruleOpt.isPresent()) {
                IpAccessControl rule = ruleOpt.get();
                ipAccessControlRepository.delete(rule);
                
                log.info("删除IP访问控制规则: {} {} by {}", rule.getAccessType(), rule.getIpAddress(), operator);
                
                // 记录安全事件
                recordSecurityEvent("IP_ACCESS_RULE_REMOVED", "INFO",
                        "删除IP访问控制规则: " + rule.getAccessType() + " " + rule.getIpAddress(),
                        null, operator, null, null, null, null,
                        "rule_id=" + ruleId);
                
                return true;
            }
            return false;
            
        } catch (Exception e) {
            log.error("删除IP访问控制规则失败", e);
            return false;
        }
    }
    
    @Override
    public Page<IpAccessControl> getIpAccessRules(String accessType, Pageable pageable) {
        try {
            if (accessType != null) {
                return ipAccessControlRepository.findByAccessTypeOrderByCreatedAtDesc(accessType, pageable);
            } else {
                return ipAccessControlRepository.findAll(pageable);
            }
        } catch (Exception e) {
            log.error("获取IP访问控制规则失败", e);
            return Page.empty();
        }
    }
    
    @Override
    public SecurityReportDto generateSecurityReport(LocalDateTime startTime, LocalDateTime endTime, String reportType) {
        try {
            if (startTime == null) {
                startTime = LocalDateTime.now().minusDays(30);
            }
            if (endTime == null) {
                endTime = LocalDateTime.now();
            }
            
            SecurityReportDto report = SecurityReportDto.builder()
                    .reportId(UUID.randomUUID().toString())
                    .title("安全分析报告")
                    .reportType(reportType)
                    .generatedAt(LocalDateTime.now())
                    .timeRange(SecurityReportDto.TimeRange.builder()
                            .startTime(startTime)
                            .endTime(endTime)
                            .build())
                    .build();
            
            // 生成安全摘要
            report.setSummary(generateSecuritySummary(startTime, endTime));
            
            // 生成威胁统计
            report.setThreats(generateThreatStatistics(startTime, endTime));
            
            // 生成登录统计
            report.setLogins(generateLoginStatistics(startTime, endTime));
            
            // 生成IP访问统计
            report.setIpAccess(generateIpAccessStatistics(startTime, endTime));
            
            // 生成安全建议
            report.setRecommendations(generateSecurityRecommendations(report));
            
            return report;
            
        } catch (Exception e) {
            log.error("生成安全报告失败", e);
            throw new RuntimeException("生成安全报告失败: " + e.getMessage());
        }
    }
    
    @Override
    public boolean detectAnomalousLogin(String userId, String sourceIp, String userAgent, String country) {
        try {
            // 简化的异常检测逻辑
            // 实际实现中可以使用机器学习算法
            
            // 检查是否来自新的地理位置
            // 检查是否使用新的设备
            // 检查登录时间是否异常
            // 检查IP信誉度
            
            // 这里返回简化的检测结果
            return false;
            
        } catch (Exception e) {
            log.error("异常登录检测失败", e);
            return false;
        }
    }
    
    @Override
    public int checkPasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            return 0;
        }
        
        int score = 0;
        
        // 长度检查
        if (password.length() >= 8) score += 20;
        if (password.length() >= 12) score += 10;
        if (password.length() >= 16) score += 10;
        
        // 字符类型检查
        if (password.matches(".*[a-z].*")) score += 10; // 小写字母
        if (password.matches(".*[A-Z].*")) score += 10; // 大写字母
        if (password.matches(".*[0-9].*")) score += 10; // 数字
        if (password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?].*")) score += 10; // 特殊字符
        
        // 复杂度检查
        Set<Character> uniqueChars = new HashSet<>();
        for (char c : password.toCharArray()) {
            uniqueChars.add(c);
        }
        if (uniqueChars.size() >= password.length() * 0.7) score += 10; // 字符多样性
        
        // 弱密码模式检查
        String lowerPassword = password.toLowerCase();
        if (COMMON_PASSWORDS.contains(lowerPassword)) {
            score -= 30;
        }
        
        for (Pattern pattern : WEAK_PATTERNS) {
            if (pattern.matcher(password).matches()) {
                score -= 20;
                break;
            }
        }
        
        return Math.max(0, Math.min(100, score));
    }
    
    @Override
    @Transactional
    public void cleanupExpiredData() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(90);
            
            // 清理过期的安全事件
            securityEventRepository.deleteByEventTimeBefore(cutoffTime);
            
            // 清理过期的IP访问控制规则
            ipAccessControlRepository.deleteByExpiresAtBefore(LocalDateTime.now());
            
            log.info("安全数据清理完成");
            
        } catch (Exception e) {
            log.error("清理过期安全数据失败", e);
        }
    }
    
    // 私有辅助方法
    
    private void setDefaultSecurityConfig(SecurityConfigDto config) {
        if (config.getEnabled() == null) {
            config.setEnabled(true);
        }
        
        if (config.getLoginSecurity() == null) {
            config.setLoginSecurity(SecurityConfigDto.LoginSecurityConfig.builder()
                    .maxFailedAttempts(5)
                    .lockoutDurationMinutes(30)
                    .enableCaptcha(true)
                    .captchaThreshold(3)
                    .enableTwoFactor(false)
                    .enableIpRestriction(false)
                    .forceSingleSession(false)
                    .build());
        }
        
        if (config.getIpAccess() == null) {
            config.setIpAccess(SecurityConfigDto.IpAccessConfig.builder()
                    .enableWhitelist(false)
                    .whitelist(new ArrayList<>())
                    .enableBlacklist(false)
                    .blacklist(new ArrayList<>())
                    .allowPrivateNetworks(true)
                    .allowedCountries(new ArrayList<>())
                    .build());
        }
    }
    
    private SecurityConfigDto getDefaultSecurityConfig() {
        SecurityConfigDto config = new SecurityConfigDto();
        setDefaultSecurityConfig(config);
        return config;
    }
    
    private Map<String, Object> buildConfigMap(SecurityConfigDto config) {
        Map<String, Object> configMap = new HashMap<>();
        
        configMap.put("enabled", config.getEnabled());
        
        if (config.getLoginSecurity() != null) {
            Map<String, Object> loginSecurity = new HashMap<>();
            SecurityConfigDto.LoginSecurityConfig ls = config.getLoginSecurity();
            loginSecurity.put("maxFailedAttempts", ls.getMaxFailedAttempts());
            loginSecurity.put("lockoutDurationMinutes", ls.getLockoutDurationMinutes());
            loginSecurity.put("enableCaptcha", ls.getEnableCaptcha());
            loginSecurity.put("captchaThreshold", ls.getCaptchaThreshold());
            loginSecurity.put("enableTwoFactor", ls.getEnableTwoFactor());
            loginSecurity.put("enableIpRestriction", ls.getEnableIpRestriction());
            loginSecurity.put("forceSingleSession", ls.getForceSingleSession());
            configMap.put("loginSecurity", loginSecurity);
        }
        
        if (config.getIpAccess() != null) {
            Map<String, Object> ipAccess = new HashMap<>();
            SecurityConfigDto.IpAccessConfig ia = config.getIpAccess();
            ipAccess.put("enableWhitelist", ia.getEnableWhitelist());
            ipAccess.put("whitelist", ia.getWhitelist());
            ipAccess.put("enableBlacklist", ia.getEnableBlacklist());
            ipAccess.put("blacklist", ia.getBlacklist());
            ipAccess.put("allowPrivateNetworks", ia.getAllowPrivateNetworks());
            ipAccess.put("allowedCountries", ia.getAllowedCountries());
            configMap.put("ipAccess", ipAccess);
        }
        
        return configMap;
    }
    
    private SecurityEventDto convertToDto(SecurityEvent event) {
        return SecurityEventDto.builder()
                .id(event.getId().toString())
                .eventType(event.getEventType())
                .severity(event.getSeverity())
                .description(event.getDescription())
                .userId(event.getUserId())
                .username(event.getUsername())
                .sourceIp(event.getSourceIp())
                .userAgent(event.getUserAgent())
                .eventTime(event.getEventTime())
                .requestPath(event.getRequestPath())
                .requestMethod(event.getRequestMethod())
                .geoLocation(SecurityEventDto.GeoLocationInfo.builder()
                        .country(event.getCountry())
                        .region(event.getRegion())
                        .city(event.getCity())
                        .isp(event.getIsp())
                        .longitude(event.getLongitude())
                        .latitude(event.getLatitude())
                        .build())
                .handled(event.getHandled())
                .resolution(event.getResolution())
                .build();
    }
    
    private void enrichGeoLocation(SecurityEvent event, String sourceIp) {
        // 简化实现，实际可以集成GeoIP服务
        if ("127.0.0.1".equals(sourceIp) || "::1".equals(sourceIp)) {
            event.setCountry("本地");
            event.setRegion("本地");
            event.setCity("本地");
        }
    }
    
    private void checkRealTimeAlert(SecurityEvent event) {
        // 检查是否需要实时告警
        if ("CRITICAL".equals(event.getSeverity())) {
            // 发送告警通知（邮件、短信等）
            log.warn("高危安全事件需要告警: {}", event.getDescription());
        }
    }
    
    private boolean isIpMatching(String clientIp, String ruleIp) {
        try {
            // 简化的IP匹配逻辑
            if (ruleIp.contains("/")) {
                // CIDR格式匹配
                return isIpInCidr(clientIp, ruleIp);
            } else {
                // 精确匹配
                return clientIp.equals(ruleIp);
            }
        } catch (Exception e) {
            log.error("IP匹配检查失败", e);
            return false;
        }
    }
    
    private boolean isIpInCidr(String ip, String cidr) {
        // 简化实现，实际需要更完整的CIDR匹配逻辑
        return ip.startsWith(cidr.split("/")[0].substring(0, cidr.split("/")[0].lastIndexOf(".")));
    }
    
    private SecurityReportDto.SecuritySummary generateSecuritySummary(LocalDateTime startTime, LocalDateTime endTime) {
        long totalEvents = securityEventRepository.countByEventTimeBetween(startTime, endTime);
        
        List<Object[]> severityStats = securityEventRepository.countBySeverityAndEventTimeBetween(startTime, endTime);
        long criticalEvents = 0, warningEvents = 0, infoEvents = 0;
        
        for (Object[] stat : severityStats) {
            String severity = (String) stat[0];
            Long count = (Long) stat[1];
            switch (severity) {
                case "CRITICAL" -> criticalEvents = count;
                case "WARNING" -> warningEvents = count;
                case "INFO" -> infoEvents = count;
            }
        }
        
        // 计算安全分数（简化算法）
        int securityScore = 100;
        if (criticalEvents > 0) securityScore -= Math.min(50, criticalEvents * 10);
        if (warningEvents > 0) securityScore -= Math.min(30, warningEvents * 2);
        
        String securityLevel = securityScore >= 80 ? "良好" : 
                              securityScore >= 60 ? "一般" : "较差";
        
        return SecurityReportDto.SecuritySummary.builder()
                .securityScore(securityScore)
                .securityLevel(securityLevel)
                .totalEvents(totalEvents)
                .criticalEvents(criticalEvents)
                .warningEvents(warningEvents)
                .infoEvents(infoEvents)
                .handledEvents(0L) // 需要从数据库查询
                .unhandledEvents(totalEvents) // 简化实现
                .build();
    }
    
    private SecurityReportDto.ThreatStatistics generateThreatStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        // 简化实现，实际需要根据事件类型统计
        return SecurityReportDto.ThreatStatistics.builder()
                .bruteForceAttempts(0L)
                .suspiciousIpAccess(0L)
                .anomalousLogins(0L)
                .apiAbuse(0L)
                .privilegeEscalation(0L)
                .dataLeakageRisks(0L)
                .build();
    }
    
    private SecurityReportDto.LoginStatistics generateLoginStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        // 简化实现
        return SecurityReportDto.LoginStatistics.builder()
                .totalLogins(0L)
                .successfulLogins(0L)
                .failedLogins(0L)
                .lockedAccounts(0L)
                .newDeviceLogins(0L)
                .remoteLogins(0L)
                .locationDistribution(new HashMap<>())
                .build();
    }
    
    private SecurityReportDto.IpAccessStatistics generateIpAccessStatistics(LocalDateTime startTime, LocalDateTime endTime) {
        List<Object[]> topIps = securityEventRepository.getTopSourceIps(startTime, endTime, 
                Pageable.ofSize(10));
        
        List<SecurityReportDto.IpAccessInfo> topAccessIps = topIps.stream()
                .map(stat -> SecurityReportDto.IpAccessInfo.builder()
                        .ipAddress((String) stat[0])
                        .accessCount((Long) stat[1])
                        .country("未知")
                        .lastAccess("")
                        .blocked(false)
                        .build())
                .collect(Collectors.toList());
        
        return SecurityReportDto.IpAccessStatistics.builder()
                .uniqueIpCount((long) topIps.size())
                .blockedIpCount(0L)
                .whitelistHits(0L)
                .blacklistHits(0L)
                .countryDistribution(new HashMap<>())
                .topAccessIps(topAccessIps)
                .build();
    }
    
    private List<SecurityReportDto.SecurityRecommendation> generateSecurityRecommendations(SecurityReportDto report) {
        List<SecurityReportDto.SecurityRecommendation> recommendations = new ArrayList<>();
        
        if (report.getSummary().getSecurityScore() < 80) {
            recommendations.add(SecurityReportDto.SecurityRecommendation.builder()
                    .id("REC001")
                    .priority("高")
                    .title("提升整体安全等级")
                    .description("当前安全分数较低，建议加强安全防护措施")
                    .action("检查并处理未解决的安全事件")
                    .riskLevel("中")
                    .impact("系统安全")
                    .build());
        }
        
        if (report.getSummary().getCriticalEvents() > 0) {
            recommendations.add(SecurityReportDto.SecurityRecommendation.builder()
                    .id("REC002")
                    .priority("紧急")
                    .title("处理高危安全事件")
                    .description("存在未处理的高危安全事件")
                    .action("立即审查和处理高危安全事件")
                    .riskLevel("高")
                    .impact("数据安全")
                    .build());
        }
        
        return recommendations;
    }
}