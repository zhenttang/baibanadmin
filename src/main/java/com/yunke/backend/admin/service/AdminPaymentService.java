package com.yunke.backend.admin.service;

import com.yunke.backend.admin.dto.AdminPaymentConfigDTO;
import com.yunke.backend.payment.dto.PaymentTestResultDTO;
import com.yunke.backend.payment.dto.PaymentProviderStatusDTO;
import com.yunke.backend.payment.dto.PaymentStatsDTO;
import java.util.List;

public interface AdminPaymentService {
    
    /**
     * 获取支付配置
     */
    AdminPaymentConfigDTO getPaymentConfig();
    
    /**
     * 更新支付配置
     */
    AdminPaymentConfigDTO updatePaymentConfig(AdminPaymentConfigDTO config);
    
    /**
     * 测试支付提供商连接
     */
    PaymentTestResultDTO testPaymentProvider(String provider);
    
    /**
     * 获取所有支付提供商状态
     */
    List<PaymentProviderStatusDTO> getPaymentProviderStatus();
    
    /**
     * 获取支付统计信息
     */
    PaymentStatsDTO getPaymentStats();
    
    /**
     * 获取支付统计信息（按日期范围）
     */
    PaymentStatsDTO getPaymentStats(String startDate, String endDate);
    
    /**
     * 刷新支付提供商状态
     */
    void refreshProviderStatus();
    
    /**
     * 启用/禁用支付提供商
     */
    void togglePaymentProvider(String provider, boolean enabled);

    /**
     * 测试支付连接
     */
    PaymentTestResultDTO testPaymentConnection(String provider, java.util.Map<String, Object> testConfig);

    /**
     * 获取提供商状态
     */
    java.util.List<PaymentProviderStatusDTO> getProvidersStatus();

    /**
     * 启用提供商
     */
    void enableProvider(String provider);

    /**
     * 禁用提供商
     */
    void disableProvider(String provider);

    /**
     * 重新加载支付配置
     */
    void reloadPaymentConfig();

    /**
     * 验证Webhook配置
     */
    java.util.Map<String, Object> verifyWebhookConfig();
}
