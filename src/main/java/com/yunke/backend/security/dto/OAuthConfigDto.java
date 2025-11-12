package com.yunke.backend.security.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;

/**
 * OAuth配置数据传输对象
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthConfigDto {
    
    /**
     * OAuth提供商标识
     */
    @NotBlank(message = "OAuth提供商不能为空")
    private String provider;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 客户端ID
     */
    private String clientId;
    
    /**
     * 客户端密钥（敏感信息，返回时需要脱敏）
     */
    private String clientSecret;
    
    /**
     * 授权URL
     */
    private String authUrl;
    
    /**
     * Token获取URL
     */
    private String tokenUrl;
    
    /**
     * 用户信息URL
     */
    private String userInfoUrl;
    
    /**
     * 权限范围
     */
    private String scope;
    
    /**
     * OIDC发行者URL（仅OIDC提供商）
     */
    private String issuer;
    
    /**
     * 用户ID声明字段（仅OIDC提供商）
     */
    private String claimId;
    
    /**
     * 邮箱声明字段（仅OIDC提供商）
     */
    private String claimEmail;
    
    /**
     * 姓名声明字段（仅OIDC提供商）
     */
    private String claimName;
    
    /**
     * 额外参数
     */
    private Map<String, String> args;
    
    /**
     * 回调URL
     */
    private String callbackUrl;
    
    /**
     * 是否已配置（只读字段）
     */
    private Boolean configured;
    
    /**
     * 连接状态（只读字段）
     */
    private String connectionStatus;
}