package com.yunke.backend.system.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.util.Map;

/**
 * 服务器配置DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerConfigDto {
    
    /**
     * 服务器名称
     */
    @NotBlank(message = "服务器名称不能为空")
    private String serverName;
    
    /**
     * 外部访问URL
     */
    @Pattern(regexp = "^https?://.*", message = "URL格式不正确")
    private String externalUrl;
    
    /**
     * 服务器主机地址
     */
    private String host;
    
    /**
     * 服务器端口
     */
    @Min(value = 1, message = "端口号不能小于1")
    @Max(value = 65535, message = "端口号不能大于65535")
    private Integer port;
    
    /**
     * 是否启用HTTPS
     */
    private Boolean httpsEnabled;
    
    /**
     * 最大上传文件大小（MB）
     */
    @Min(value = 1, message = "最大上传大小不能小于1MB")
    private Integer maxUploadSize;
    
    /**
     * 会话超时时间（分钟）
     */
    @Min(value = 5, message = "会话超时时间不能小于5分钟")
    private Integer sessionTimeout;
    
    /**
     * 是否启用注册
     */
    private Boolean enableSignup;
    
    /**
     * 是否启用邀请码
     */
    private Boolean enableInviteCode;
    
    /**
     * 默认语言
     */
    private String defaultLanguage;
    
    /**
     * 时区设置
     */
    private String timezone;
    
    /**
     * 是否启用维护模式
     */
    private Boolean maintenanceMode;
    
    /**
     * 维护提示信息
     */
    private String maintenanceMessage;
    
    /**
     * 扩展配置
     */
    private Map<String, Object> extensions;
}