package com.yunke.backend.security.service;

import com.yunke.backend.security.dto.SecurityConfigDto;
import com.yunke.backend.security.dto.SecurityEventDto;
import com.yunke.backend.security.dto.SecurityReportDto;
import com.yunke.backend.system.domain.entity.IpAccessControl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 安全管理服务接口
 */
public interface SecurityService {
    
    /**
     * 获取安全配置
     * @return 安全配置
     */
    SecurityConfigDto getSecurityConfig();
    
    /**
     * 更新安全配置
     * @param config 配置信息
     * @param operator 操作人
     * @param sourceIp 来源IP
     * @return 更新后的配置
     */
    SecurityConfigDto updateSecurityConfig(SecurityConfigDto config, String operator, String sourceIp);
    
    /**
     * 验证安全配置
     * @param config 配置信息
     * @return 验证结果
     */
    boolean validateSecurityConfig(SecurityConfigDto config);
    
    /**
     * 记录安全事件
     * @param eventType 事件类型
     * @param severity 严重程度
     * @param description 描述
     * @param userId 用户ID
     * @param username 用户名
     * @param sourceIp 来源IP
     * @param userAgent 用户代理
     * @param requestPath 请求路径
     * @param requestMethod 请求方法
     * @param details 详细信息
     */
    void recordSecurityEvent(String eventType, String severity, String description,
                           String userId, String username, String sourceIp,
                           String userAgent, String requestPath, String requestMethod,
                           String details);
    
    /**
     * 获取安全事件列表
     * @param eventType 事件类型
     * @param severity 严重程度
     * @param handled 是否已处理
     * @param sourceIp 来源IP
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param pageable 分页信息
     * @return 安全事件列表
     */
    Page<SecurityEventDto> getSecurityEvents(String eventType, String severity, Boolean handled,
                                           String sourceIp, LocalDateTime startTime, LocalDateTime endTime,
                                           Pageable pageable);
    
    /**
     * 处理安全事件
     * @param eventId 事件ID
     * @param resolution 处理结果
     * @param operator 处理人
     * @return 是否成功
     */
    boolean handleSecurityEvent(Long eventId, String resolution, String operator);
    
    /**
     * 检查IP是否被允许访问
     * @param ipAddress IP地址
     * @return 是否允许
     */
    boolean isIpAllowed(String ipAddress);
    
    /**
     * 添加IP访问控制规则
     * @param ipAddress IP地址
     * @param accessType 访问类型 (WHITELIST/BLACKLIST)
     * @param description 描述
     * @param expiresAt 过期时间
     * @param operator 操作人
     * @return IP访问控制规则
     */
    IpAccessControl addIpAccessRule(String ipAddress, String accessType, String description,
                                  LocalDateTime expiresAt, String operator);
    
    /**
     * 删除IP访问控制规则
     * @param ruleId 规则ID
     * @param operator 操作人
     * @return 是否成功
     */
    boolean removeIpAccessRule(Long ruleId, String operator);
    
    /**
     * 获取IP访问控制规则列表
     * @param accessType 访问类型
     * @param pageable 分页信息
     * @return 规则列表
     */
    Page<IpAccessControl> getIpAccessRules(String accessType, Pageable pageable);
    
    /**
     * 生成安全报告
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param reportType 报告类型
     * @return 安全报告
     */
    SecurityReportDto generateSecurityReport(LocalDateTime startTime, LocalDateTime endTime, String reportType);
    
    /**
     * 检测异常登录
     * @param userId 用户ID
     * @param sourceIp IP地址
     * @param userAgent 用户代理
     * @param country 国家
     * @return 是否异常
     */
    boolean detectAnomalousLogin(String userId, String sourceIp, String userAgent, String country);
    
    /**
     * 检查密码强度
     * @param password 密码
     * @return 强度评分 (0-100)
     */
    int checkPasswordStrength(String password);
    
    /**
     * 清理过期数据
     */
    void cleanupExpiredData();
}