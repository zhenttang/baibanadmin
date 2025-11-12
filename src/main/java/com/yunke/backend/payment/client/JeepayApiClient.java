package com.yunke.backend.payment.client;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.yunke.backend.payment.dto.jeepay.*;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.TreeMap;

/**
 * Jeepay API客户端
 */
@Slf4j
@Builder
public class JeepayApiClient {
    
    private final String apiUrl;
    private final String mchNo;
    private final String appId;
    private final String appSecret;
    private final String signType;
    private final Integer connectTimeout;
    private final Integer readTimeout;
    
    private final CloseableHttpClient httpClient;
    
    public JeepayApiClient(String apiUrl, String mchNo, String appId, String appSecret, 
                          String signType, Integer connectTimeout, Integer readTimeout, 
                          CloseableHttpClient httpClient) {
        this.apiUrl = apiUrl;
        this.mchNo = mchNo;
        this.appId = appId;
        this.appSecret = appSecret;
        this.signType = signType;
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
        
        // 创建HTTP客户端配置
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(connectTimeout)
            .setSocketTimeout(readTimeout)
            .build();
            
        this.httpClient = httpClient != null ? httpClient : 
            HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build();
    }
    
    /**
     * 统一下单接口
     */
    public ApiResponse<UnifiedOrderResponse> unifiedOrder(UnifiedOrderRequest request) {
        try {
            // 设置基础参数
            request.setMchNo(mchNo);
            request.setAppId(appId);
            request.setSign(generateSign(request));
            
            String url = apiUrl + "/api/pay/unifiedOrder";
            String responseStr = doPost(url, JSON.toJSONString(request));
            
            log.info("Jeepay统一下单响应: {}", responseStr);
            
            return JSON.parseObject(responseStr, ApiResponse.class);
            
        } catch (Exception e) {
            log.error("统一下单请求失败", e);
            return ApiResponse.<UnifiedOrderResponse>builder()
                .code(500)
                .msg("统一下单请求失败: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * 订单查询接口
     */
    public ApiResponse<PayOrderQueryResponse> payOrderQuery(PayOrderQueryRequest request) {
        try {
            request.setMchNo(mchNo);
            request.setAppId(appId);
            request.setSign(generateSign(request));
            
            String url = apiUrl + "/api/pay/query";
            String responseStr = doPost(url, JSON.toJSONString(request));
            
            log.info("Jeepay订单查询响应: {}", responseStr);
            
            return JSON.parseObject(responseStr, ApiResponse.class);
            
        } catch (Exception e) {
            log.error("订单查询请求失败", e);
            return ApiResponse.<PayOrderQueryResponse>builder()
                .code(500)
                .msg("订单查询请求失败: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * 关闭订单接口
     */
    public ApiResponse<Void> closeOrder(CloseOrderRequest request) {
        try {
            request.setMchNo(mchNo);
            request.setAppId(appId);
            request.setSign(generateSign(request));
            
            String url = apiUrl + "/api/pay/close";
            String responseStr = doPost(url, JSON.toJSONString(request));
            
            log.info("Jeepay关闭订单响应: {}", responseStr);
            
            return JSON.parseObject(responseStr, ApiResponse.class);
            
        } catch (Exception e) {
            log.error("关闭订单请求失败", e);
            return ApiResponse.<Void>builder()
                .code(500)
                .msg("关闭订单请求失败: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * 退款接口
     */
    public ApiResponse<RefundOrderResponse> refundOrder(RefundOrderRequest request) {
        try {
            request.setMchNo(mchNo);
            request.setAppId(appId);
            request.setSign(generateSign(request));
            
            String url = apiUrl + "/api/refund/refundOrder";
            String responseStr = doPost(url, JSON.toJSONString(request));
            
            log.info("Jeepay退款响应: {}", responseStr);
            
            return JSON.parseObject(responseStr, ApiResponse.class);
            
        } catch (Exception e) {
            log.error("退款请求失败", e);
            return ApiResponse.<RefundOrderResponse>builder()
                .code(500)
                .msg("退款请求失败: " + e.getMessage())
                .build();
        }
    }
    
    /**
     * 验证回调签名
     */
    public boolean verifyNotifySignature(String notifyData, String signature) {
        try {
            JSONObject notifyObj = JSON.parseObject(notifyData);
            String calculatedSign = generateSign(notifyObj);
            return calculatedSign.equals(signature);
        } catch (Exception e) {
            log.error("验证回调签名失败", e);
            return false;
        }
    }
    
    /**
     * 生成签名
     */
    private String generateSign(Object obj) {
        JSONObject jsonObj = JSON.parseObject(JSON.toJSONString(obj));
        return generateSign(jsonObj);
    }
    
    private String generateSign(JSONObject params) {
        // 移除sign参数
        params.remove("sign");
        
        // 按字典序排序
        Map<String, Object> sortedMap = new TreeMap<>(params);
        
        // 构建签名字符串
        StringBuilder signStr = new StringBuilder();
        for (Map.Entry<String, Object> entry : sortedMap.entrySet()) {
            if (entry.getValue() != null && !entry.getValue().toString().isEmpty()) {
                if (signStr.length() > 0) {
                    signStr.append("&");
                }
                signStr.append(entry.getKey()).append("=").append(entry.getValue());
            }
        }
        
        // 添加密钥
        signStr.append("&key=").append(appSecret);
        
        // MD5签名
        String sign = DigestUtils.md5Hex(signStr.toString()).toUpperCase();
        
        log.debug("签名字符串: {}", signStr);
        log.debug("生成签名: {}", sign);
        
        return sign;
    }
    
    /**
     * 发送POST请求
     */
    private String doPost(String url, String jsonBody) throws IOException {
        HttpPost httpPost = new HttpPost(url);
        httpPost.setHeader("Content-Type", "application/json; charset=UTF-8");
        httpPost.setEntity(new StringEntity(jsonBody, StandardCharsets.UTF_8));
        
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            HttpEntity entity = response.getEntity();
            return EntityUtils.toString(entity, StandardCharsets.UTF_8);
        }
    }
    
    /**
     * 关闭HTTP客户端
     */
    public void close() throws IOException {
        if (httpClient != null) {
            httpClient.close();
        }
    }
}