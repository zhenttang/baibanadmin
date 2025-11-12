package com.yunke.backend.system.service;

import com.yunke.backend.system.dto.ServerConfigDto;
import com.yunke.backend.system.dto.ServerInfoDto;
import com.yunke.backend.system.dto.ConfigOperationLogDto;
import java.util.List;

/**
 * 服务器配置管理服务接口
 */
public interface ServerConfigService {
    
    /**
     * 获取服务器完整信息
     * @return 服务器信息
     */
    ServerInfoDto getServerInfo();
    
    /**
     * 获取服务器配置
     * @return 服务器配置
     */
    ServerConfigDto getServerConfig();
    
    /**
     * 更新服务器配置
     * @param config 配置信息
     * @param operator 操作用户
     * @param sourceIp 来源IP
     * @return 更新后的配置
     */
    ServerConfigDto updateServerConfig(ServerConfigDto config, String operator, String sourceIp);
    
    /**
     * 验证服务器配置
     * @param config 配置信息
     * @return 验证结果
     */
    boolean validateServerConfig(ServerConfigDto config);
    
    /**
     * 重新加载配置
     * @return 是否成功
     */
    boolean reloadConfig();
    
    /**
     * 获取配置操作日志
     * @param moduleName 模块名称
     * @param limit 限制数量
     * @return 操作日志列表
     */
    List<ConfigOperationLogDto> getConfigOperationLogs(String moduleName, Integer limit);
    
    /**
     * 获取系统健康状态
     * @return 健康状态
     */
    ServerInfoDto.ServiceStatus getSystemHealth();
}