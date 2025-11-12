package com.yunke.backend.system.service.impl;

import com.yunke.backend.infrastructure.config.AuthConfig;
import com.yunke.backend.security.dto.OAuthConfigDto;
import com.yunke.backend.security.dto.OAuthTestResultDto;
import com.yunke.backend.security.dto.OAuthStatisticsDto;
import com.yunke.backend.system.domain.entity.ConnectedAccount;
import com.yunke.backend.system.repository.ConnectedAccountRepository;
import com.yunke.backend.system.service.ConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * OAuth配置服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthConfigServiceImpl {
    
    private final AuthConfig authConfig;
    private final ConfigService configService;
    private final ConnectedAccountRepository connectedAccountRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    
    private static final Map<String, String> PROVIDER_DISPLAY_NAMES = Map.of(
        "google", "Google",
        "github", "GitHub", 
        "apple", "Apple",
        "oidc", "OIDC"
    );
    
    /**
     * 获取所有OAuth配置
     */
    public List<OAuthConfigDto> getAllConfigs() {
        List<OAuthConfigDto> configs = new ArrayList<>();
        
        // Google配置
        configs.add(convertToDto("google", authConfig.getOauth().getGoogle()));
        
        // GitHub配置  
        configs.add(convertToDto("github", authConfig.getOauth().getGithub()));
        
        // Apple配置
        configs.add(convertToDto("apple", authConfig.getOauth().getApple()));
        
        // OIDC配置
        configs.add(convertToDto("oidc", authConfig.getOauth().getOidc()));
        
        return configs;
    }
    
    /**
     * 获取特定提供商的配置
     */
    public OAuthConfigDto getConfig(String provider) {
        return switch (provider.toLowerCase()) {
            case "google" -> convertToDto("google", authConfig.getOauth().getGoogle());
            case "github" -> convertToDto("github", authConfig.getOauth().getGithub());
            case "apple" -> convertToDto("apple", authConfig.getOauth().getApple());
            case "oidc" -> convertToDto("oidc", authConfig.getOauth().getOidc());
            default -> throw new IllegalArgumentException("不支持的OAuth提供商: " + provider);
        };
    }
    
    /**
     * 更新OAuth配置
     */
    public void updateConfig(String provider, OAuthConfigDto configDto) {
        try {
            switch (provider.toLowerCase()) {
                case "google" -> updateGoogleConfig(configDto);
                case "github" -> updateGithubConfig(configDto);
                case "apple" -> updateAppleConfig(configDto);
                case "oidc" -> updateOidcConfig(configDto);
                default -> throw new IllegalArgumentException("不支持的OAuth提供商: " + provider);
            }
            
            log.info("OAuth配置更新成功: {}", provider);
        } catch (Exception e) {
            log.error("更新OAuth配置失败: {}", provider, e);
            throw new RuntimeException("更新OAuth配置失败: " + e.getMessage());
        }
    }
    
    /**
     * 测试OAuth连接
     */
    public OAuthTestResultDto testConnection(String provider) {
        long startTime = System.currentTimeMillis();
        
        try {
            OAuthConfigDto config = getConfig(provider);
            
            if (!config.getEnabled()) {
                return OAuthTestResultDto.failure("OAuth提供商未启用", "请先启用" + provider + "OAuth配置");
            }
            
            if (config.getClientId() == null || config.getClientSecret() == null) {
                return OAuthTestResultDto.failure("OAuth配置不完整", "缺少客户端ID或密钥");
            }
            
            // 测试授权URL生成
            String authUrl = generateAuthUrl(provider, config);
            log.info("生成授权URL成功: {}", authUrl);
            
            // 这里可以添加更多测试步骤，比如：
            // 1. 验证授权端点可访问性
            // 2. 测试Token端点响应
            // 3. 验证用户信息端点
            
            boolean endpointReachable = testEndpointReachability(config.getAuthUrl());
            
            long responseTime = System.currentTimeMillis() - startTime;
            
            if (endpointReachable) {
                Map<String, Object> testResult = Map.of(
                    "authUrl", authUrl,
                    "endpointStatus", "可访问",
                    "provider", PROVIDER_DISPLAY_NAMES.get(provider)
                );
                
                return OAuthTestResultDto.success(
                    provider + " OAuth配置测试成功", 
                    responseTime, 
                    testResult
                );
            } else {
                return OAuthTestResultDto.failure(
                    "OAuth端点不可访问", 
                    "无法连接到" + config.getAuthUrl()
                );
            }
            
        } catch (Exception e) {
            log.error("OAuth连接测试失败: {}", provider, e);
            long responseTime = System.currentTimeMillis() - startTime;
            return OAuthTestResultDto.failure(
                "OAuth连接测试失败", 
                e.getMessage()
            );
        }
    }
    
    /**
     * 生成回调URL
     */
    public String generateCallbackUrl(String provider) {
        // 这里应该根据实际的服务器配置生成回调URL
        Object externalUrl = configService.getConfigValue("server", "externalUrl");
        String baseUrl = externalUrl != null ? externalUrl.toString() : "http://localhost:3000";
        return baseUrl + "/api/auth/oauth/callback/" + provider;
    }
    
    /**
     * 获取OAuth统计信息
     */
    public OAuthStatisticsDto getStatistics() {
        try {
            // 获取总体统计
            Long totalLogins = connectedAccountRepository.countTotalLogins();
            Long successfulLogins = connectedAccountRepository.countSuccessfulLogins();
            Long failedLogins = totalLogins - successfulLogins;
            Double successRate = totalLogins > 0 ? (successfulLogins.doubleValue() / totalLogins) * 100 : 0.0;
            
            // 获取各提供商统计
            Map<String, OAuthStatisticsDto.ProviderStats> providerStats = getProviderStatistics();
            
            // 获取最近7天趋势
            Map<LocalDate, Long> weeklyTrend = getWeeklyLoginTrend();
            
            // 获取用户统计
            Long activeUsers = connectedAccountRepository.countActiveUsers();
            Long newUsers = connectedAccountRepository.countNewUsersThisMonth();
            
            OAuthStatisticsDto statistics = new OAuthStatisticsDto();
            statistics.setTotalLogins(totalLogins);
            statistics.setSuccessfulLogins(successfulLogins);
            statistics.setFailedLogins(failedLogins);
            statistics.setSuccessRate(Math.round(successRate * 100.0) / 100.0);
            statistics.setProviderStats(providerStats);
            statistics.setWeeklyTrend(weeklyTrend);
            statistics.setActiveUsers(activeUsers);
            statistics.setNewUsers(newUsers);
            statistics.setLastUpdated(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return statistics;
        } catch (Exception e) {
            log.error("获取OAuth统计信息失败", e);
            throw new RuntimeException("获取OAuth统计信息失败: " + e.getMessage());
        }
    }
    
    // 私有辅助方法
    
    private OAuthConfigDto convertToDto(String provider, AuthConfig.OAuthProvider config) {
        OAuthConfigDto dto = new OAuthConfigDto();
        dto.setProvider(provider);
        dto.setEnabled(config.isEnabled());
        dto.setClientId(config.getClientId());
        // 脱敏客户端密钥
        dto.setClientSecret(config.getClientSecret() != null ? "***" : null);
        dto.setAuthUrl(config.getAuthUrl());
        dto.setTokenUrl(config.getTokenUrl());
        dto.setUserInfoUrl(config.getUserInfoUrl());
        dto.setScope(config.getScope());
        dto.setArgs(config.getArgs());
        dto.setCallbackUrl(generateCallbackUrl(provider));
        dto.setConfigured(isConfigured(config));
        dto.setConnectionStatus(getConnectionStatus(provider, config));
        
        // 处理OIDC特殊字段
        if (config instanceof AuthConfig.OidcProvider oidcConfig) {
            dto.setIssuer(oidcConfig.getIssuer());
            dto.setClaimId(oidcConfig.getClaimId());
            dto.setClaimEmail(oidcConfig.getClaimEmail());
            dto.setClaimName(oidcConfig.getClaimName());
        }
        
        return dto;
    }
    
    private void updateGoogleConfig(OAuthConfigDto dto) {
        AuthConfig.OAuthProvider google = authConfig.getOauth().getGoogle();
        updateProviderConfig(google, dto);
    }
    
    private void updateGithubConfig(OAuthConfigDto dto) {
        AuthConfig.OAuthProvider github = authConfig.getOauth().getGithub();
        updateProviderConfig(github, dto);
    }
    
    private void updateAppleConfig(OAuthConfigDto dto) {
        AuthConfig.OAuthProvider apple = authConfig.getOauth().getApple();
        updateProviderConfig(apple, dto);
    }
    
    private void updateOidcConfig(OAuthConfigDto dto) {
        AuthConfig.OidcProvider oidc = authConfig.getOauth().getOidc();
        updateProviderConfig(oidc, dto);
        
        // 更新OIDC特殊字段
        if (dto.getIssuer() != null) oidc.setIssuer(dto.getIssuer());
        if (dto.getClaimId() != null) oidc.setClaimId(dto.getClaimId());
        if (dto.getClaimEmail() != null) oidc.setClaimEmail(dto.getClaimEmail());
        if (dto.getClaimName() != null) oidc.setClaimName(dto.getClaimName());
    }
    
    private void updateProviderConfig(AuthConfig.OAuthProvider provider, OAuthConfigDto dto) {
        if (dto.getEnabled() != null) provider.setEnabled(dto.getEnabled());
        if (dto.getClientId() != null) provider.setClientId(dto.getClientId());
        if (dto.getClientSecret() != null && !"***".equals(dto.getClientSecret())) {
            provider.setClientSecret(dto.getClientSecret());
        }
        if (dto.getAuthUrl() != null) provider.setAuthUrl(dto.getAuthUrl());
        if (dto.getTokenUrl() != null) provider.setTokenUrl(dto.getTokenUrl());
        if (dto.getUserInfoUrl() != null) provider.setUserInfoUrl(dto.getUserInfoUrl());
        if (dto.getScope() != null) provider.setScope(dto.getScope());
        if (dto.getArgs() != null) provider.setArgs(dto.getArgs());
    }
    
    private String generateAuthUrl(String provider, OAuthConfigDto config) {
        String state = UUID.randomUUID().toString();
        String redirectUri = config.getCallbackUrl();
        
        return config.getAuthUrl() + 
               "?client_id=" + config.getClientId() +
               "&redirect_uri=" + redirectUri +
               "&scope=" + config.getScope() +
               "&state=" + state +
               "&response_type=code";
    }
    
    private boolean testEndpointReachability(String url) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "AFFiNE-OAuth-Test/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);
            
            ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, entity, String.class);
            
            return response.getStatusCode().is2xxSuccessful() || 
                   response.getStatusCode().is3xxRedirection();
        } catch (Exception e) {
            log.debug("端点可访问性测试失败: {}", url, e);
            return false;
        }
    }
    
    private boolean isConfigured(AuthConfig.OAuthProvider config) {
        return config.getClientId() != null && 
               config.getClientSecret() != null &&
               config.getAuthUrl() != null &&
               config.getTokenUrl() != null;
    }
    
    private String getConnectionStatus(String provider, AuthConfig.OAuthProvider config) {
        try {
            if (!config.isEnabled()) {
                return "已禁用";
            }
            if (!isConfigured(config)) {
                return "未配置";
            }

            // 简单的连接性检查
            String authUrl = config.getAuthUrl();
            if (authUrl == null || authUrl.isBlank()) {
                return "未配置";
            }

            boolean reachable = testEndpointReachability(authUrl);
            return reachable ? "正常" : "无法连接";
        } catch (Exception e) {
            log.debug("获取{}连接状态失败", provider, e);
            return "错误";
        }
    }
    
    private Map<String, OAuthStatisticsDto.ProviderStats> getProviderStatistics() {
        Map<String, OAuthStatisticsDto.ProviderStats> stats = new HashMap<>();
        
        for (String provider : PROVIDER_DISPLAY_NAMES.keySet()) {
            try {
                Long loginCount = connectedAccountRepository.countLoginsByProvider(provider);
                Long userCount = connectedAccountRepository.countUsersByProvider(provider);
                OAuthConfigDto config = getConfig(provider);
                
                OAuthStatisticsDto.ProviderStats providerStats = new OAuthStatisticsDto.ProviderStats();
                providerStats.setProvider(PROVIDER_DISPLAY_NAMES.get(provider));
                providerStats.setLoginCount(loginCount);
                providerStats.setSuccessCount(loginCount); // 简化处理
                providerStats.setFailureCount(0L);
                providerStats.setUserCount(userCount);
                providerStats.setEnabled(config.getEnabled());
                providerStats.setConfigStatus(config.getConfigured() ? "已配置" : "未配置");
                
                stats.put(provider, providerStats);
            } catch (Exception e) {
                log.warn("获取{}提供商统计失败", provider, e);
            }
        }
        
        return stats;
    }
    
    private Map<LocalDate, Long> getWeeklyLoginTrend() {
        Map<LocalDate, Long> trend = new LinkedHashMap<>();
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6);
        
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            try {
                Long count = connectedAccountRepository.countLoginsByDate(date);
                trend.put(date, count);
            } catch (Exception e) {
                log.warn("获取{}日期登录统计失败", date, e);
                trend.put(date, 0L);
            }
        }
        
        return trend;
    }
}
