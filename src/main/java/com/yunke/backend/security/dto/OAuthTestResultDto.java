package com.yunke.backend.security.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

/**
 * OAuth连接测试结果DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OAuthTestResultDto {
    
    /**
     * 测试是否成功
     */
    private Boolean success;
    
    /**
     * 测试消息
     */
    private String message;
    
    /**
     * 响应时间（毫秒）
     */
    private Long responseTimeMs;
    
    /**
     * 错误详情
     */
    private String errorDetails;
    
    /**
     * 测试步骤详情
     */
    private TestStepDetails stepDetails;
    
    /**
     * 获取到的用户信息（测试成功时）
     */
    private Object userInfo;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestStepDetails {
        /**
         * 授权URL生成
         */
        private Boolean authUrlGeneration;
        
        /**
         * Token获取
         */
        private Boolean tokenAcquisition;
        
        /**
         * 用户信息获取
         */
        private Boolean userInfoRetrieval;
        
        /**
         * 详细步骤信息
         */
        private String stepInfo;
    }
    
    public static OAuthTestResultDto success(String message, Long responseTime, Object userInfo) {
        OAuthTestResultDto result = new OAuthTestResultDto();
        result.setSuccess(true);
        result.setMessage(message);
        result.setResponseTimeMs(responseTime);
        result.setUserInfo(userInfo);
        
        TestStepDetails stepDetails = new TestStepDetails();
        stepDetails.setAuthUrlGeneration(true);
        stepDetails.setTokenAcquisition(true);
        stepDetails.setUserInfoRetrieval(true);
        stepDetails.setStepInfo("所有测试步骤都成功完成");
        result.setStepDetails(stepDetails);
        
        return result;
    }
    
    public static OAuthTestResultDto failure(String message, String errorDetails) {
        OAuthTestResultDto result = new OAuthTestResultDto();
        result.setSuccess(false);
        result.setMessage(message);
        result.setErrorDetails(errorDetails);
        
        TestStepDetails stepDetails = new TestStepDetails();
        stepDetails.setAuthUrlGeneration(false);
        stepDetails.setTokenAcquisition(false);
        stepDetails.setUserInfoRetrieval(false);
        stepDetails.setStepInfo("测试失败");
        result.setStepDetails(stepDetails);
        
        return result;
    }
}