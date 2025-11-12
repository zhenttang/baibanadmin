package com.yunke.backend.security.service;

import com.yunke.backend.infrastructure.config.SecurityProtectionConfig;
import com.yunke.backend.security.dto.security.BlockedIp;
import com.yunke.backend.security.dto.security.SecurityEvent;
import com.yunke.backend.security.dto.security.SecurityStats;
import com.yunke.backend.security.enums.SecurityEventType;
import com.yunke.backend.security.enums.SecurityLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 🚨 安全监控服务
 * 负责记录、监控和统计所有安全事件
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityMonitorService {
    
    private final RedisTemplate<String, Object> redisTemplate;
    private final SecurityProtectionConfig config;
    
    /**
     * 记录安全事件
     */
    public void recordSecurityEvent(SecurityEvent event) {
        try {
            // 1. 存储到Redis（用于实时查询）
            String key = "security:events:" + LocalDate.now();
            redisTemplate.opsForList().leftPush(key, event);
            redisTemplate.expire(key, 30, TimeUnit.DAYS);
            
            // 2. 统计攻击类型
            String statsKey = "security:stats:" + event.getType();
            redisTemplate.opsForValue().increment(statsKey);
            
            // 3. 记录每小时的事件数
            String hourKey = "security:hourly:" + LocalDateTime.now().getHour();
            redisTemplate.opsForValue().increment(hourKey);
            redisTemplate.expire(hourKey, 24, TimeUnit.HOURS);
            
            // 4. 高危事件立即告警
            if (event.getLevel() == SecurityLevel.HIGH || 
                event.getLevel() == SecurityLevel.CRITICAL) {
                sendAlert(event);
            }
            
            // 5. 记录日志
            if (config.getAlert().isLogEnabled()) {
                logSecurityEvent(event);
            }
            
        } catch (Exception e) {
            log.error("记录安全事件失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 获取安全统计信息
     */
    public SecurityStats getStats() {
        try {
            // 1. 统计今日事件总数
            String todayKey = "security:events:" + LocalDate.now();
            Long todayEvents = redisTemplate.opsForList().size(todayKey);
            
            // 2. 统计各类攻击次数
            Map<String, Long> attacksByType = new HashMap<>();
            for (SecurityEventType type : SecurityEventType.values()) {
                String statsKey = "security:stats:" + type;
                Object count = redisTemplate.opsForValue().get(statsKey);
                attacksByType.put(type.name(), 
                    count != null ? Long.parseLong(count.toString()) : 0L);
            }
            
            // 3. 统计当前被封禁的IP数量
            Set<String> blockedKeys = redisTemplate.keys("security:blocked:ip:*");
            int blockedCount = blockedKeys != null ? blockedKeys.size() : 0;
            
            // 4. 统计今日封禁的IP数量
            String todayBlockKey = "security:blocked:today:" + LocalDate.now();
            Object todayBlocked = redisTemplate.opsForValue().get(todayBlockKey);
            
            // 5. 统计最近1小时的事件数
            int currentHour = LocalDateTime.now().getHour();
            String hourKey = "security:hourly:" + currentHour;
            Object hourCount = redisTemplate.opsForValue().get(hourKey);
            
            return SecurityStats.builder()
                .todayEvents(todayEvents != null ? todayEvents : 0L)
                .attacksByType(attacksByType)
                .blockedIpCount(blockedCount)
                .todayBlockedIps(todayBlocked != null ? 
                    Integer.parseInt(todayBlocked.toString()) : 0)
                .lastHourEvents(hourCount != null ? 
                    Long.parseLong(hourCount.toString()) : 0L)
                .build();
                
        } catch (Exception e) {
            log.error("获取安全统计失败: {}", e.getMessage(), e);
            return SecurityStats.builder()
                .todayEvents(0L)
                .attacksByType(new HashMap<>())
                .blockedIpCount(0)
                .build();
        }
    }
    
    /**
     * 获取最近的安全事件
     */
    public List<SecurityEvent> getRecentEvents(int days, int limit) {
        List<SecurityEvent> allEvents = new ArrayList<>();
        
        try {
            for (int i = 0; i < days; i++) {
                LocalDate date = LocalDate.now().minusDays(i);
                String key = "security:events:" + date;
                
                List<Object> events = redisTemplate.opsForList().range(key, 0, limit - 1);
                if (events != null) {
                    events.forEach(event -> {
                        if (event instanceof SecurityEvent) {
                            allEvents.add((SecurityEvent) event);
                        }
                    });
                }
                
                if (allEvents.size() >= limit) {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("获取安全事件失败: {}", e.getMessage(), e);
        }
        
        return allEvents.stream()
            .sorted(Comparator.comparing(SecurityEvent::getTimestamp).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * 获取被封禁的IP列表
     */
    public List<BlockedIp> getBlockedIps() {
        List<BlockedIp> blockedIps = new ArrayList<>();
        
        try {
            Set<String> keys = redisTemplate.keys("security:blocked:ip:*");
            
            if (keys != null) {
                for (String key : keys) {
                    String ip = key.replace("security:blocked:ip:", "");
                    Long ttl = redisTemplate.getExpire(key, TimeUnit.MINUTES);
                    String reason = (String) redisTemplate.opsForValue().get(key);
                    
                    blockedIps.add(BlockedIp.builder()
                        .ip(ip)
                        .reason(reason != null ? reason : "未知")
                        .remainingMinutes(ttl != null ? ttl : 0L)
                        .blockedAt(LocalDateTime.now().format(
                            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                        .build());
                }
            }
        } catch (Exception e) {
            log.error("获取封禁IP列表失败: {}", e.getMessage(), e);
        }
        
        return blockedIps.stream()
            .sorted(Comparator.comparing(BlockedIp::getRemainingMinutes).reversed())
            .collect(Collectors.toList());
    }
    
    /**
     * 封禁IP
     */
    public void blockIp(String ip, String reason, int durationMinutes) {
        try {
            String key = "security:blocked:ip:" + ip;
            redisTemplate.opsForValue().set(key, reason, durationMinutes, TimeUnit.MINUTES);
            
            // 统计今日封禁数
            String todayKey = "security:blocked:today:" + LocalDate.now();
            redisTemplate.opsForValue().increment(todayKey);
            redisTemplate.expire(todayKey, 1, TimeUnit.DAYS);
            
            log.warn("🚫 IP已被封禁: {}, 原因: {}, 时长: {}分钟", ip, reason, durationMinutes);
            
            // 记录封禁事件
            recordSecurityEvent(SecurityEvent.builder()
                .type(SecurityEventType.DDOS)
                .level(SecurityLevel.HIGH)
                .ip(ip)
                .details("IP被封禁: " + reason)
                .action("BLOCKED")
                .build());
                
        } catch (Exception e) {
            log.error("封禁IP失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 解封IP
     */
    public void unblockIp(String ip) {
        try {
            String key = "security:blocked:ip:" + ip;
            redisTemplate.delete(key);
            
            // 清除相关计数
            redisTemplate.delete("security:req_count:" + ip);
            redisTemplate.delete("security:login_fail_ip:" + ip);
            redisTemplate.delete("security:page_visit:" + ip);
            
            log.info("✅ IP已解封: {}", ip);
            
        } catch (Exception e) {
            log.error("解封IP失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 检查IP是否被封禁
     */
    public boolean isIpBlocked(String ip) {
        try {
            String key = "security:blocked:ip:" + ip;
            return Boolean.TRUE.equals(redisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查IP封禁状态失败: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * 发送告警
     */
    private void sendAlert(SecurityEvent event) {
        if (!config.getAlert().isEnabled()) {
            return;
        }
        
        try {
            String alertMessage = String.format(
                "🚨 安全告警\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━\n" +
                "时间: %s\n" +
                "类型: %s\n" +
                "级别: %s\n" +
                "IP: %s\n" +
                "路径: %s\n" +
                "详情: %s\n" +
                "━━━━━━━━━━━━━━━━━━━━━━━━",
                event.getTimestamp().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")),
                event.getType(),
                event.getLevel(),
                event.getIp(),
                event.getRequestPath(),
                event.getDetails()
            );
            
            // TODO: 这里可以集成邮件、短信、钉钉等告警方式
            if (config.getAlert().isEmailEnabled() && 
                config.getAlert().getAdminEmails().length > 0) {
                // 发送邮件告警（需要实现）
                log.info("📧 发送邮件告警给管理员");
            }
            
            // 记录告警日志
            log.error("🚨 {}", alertMessage);
            
        } catch (Exception e) {
            log.error("发送告警失败: {}", e.getMessage(), e);
        }
    }
    
    /**
     * 记录安全事件日志
     */
    private void logSecurityEvent(SecurityEvent event) {
        String emoji = getEmojiForLevel(event.getLevel());
        log.warn("{} 安全事件 - 类型: {}, 级别: {}, IP: {}, 路径: {}, 详情: {}", 
            emoji,
            event.getType(),
            event.getLevel(),
            event.getIp(),
            event.getRequestPath(),
            event.getDetails()
        );
    }
    
    /**
     * 根据安全级别获取表情符号
     */
    private String getEmojiForLevel(SecurityLevel level) {
        return switch (level) {
            case LOW -> "🟢";
            case MEDIUM -> "🟡";
            case HIGH -> "🔴";
            case CRITICAL -> "🚨";
        };
    }
}

