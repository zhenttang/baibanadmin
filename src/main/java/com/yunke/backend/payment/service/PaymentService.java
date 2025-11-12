package com.yunke.backend.payment.service;

import com.yunke.backend.payment.dto.request.PaymentCallbackRequest;
import com.yunke.backend.payment.dto.request.PaymentOrderRequest;
import com.yunke.backend.payment.dto.response.PaymentOrderResponse;
import com.yunke.backend.payment.dto.response.PaymentStatusResponse;
import com.yunke.backend.payment.domain.entity.PaymentRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

/**
 * 支付服务接口
 */
public interface PaymentService {
    
    /**
     * 创建支付订单
     * 
     * @param userId 用户ID
     * @param request 支付订单请求
     * @return 支付订单响应
     * @throws RuntimeException 如果文档不存在或已支付
     */
    PaymentOrderResponse createPaymentOrder(String userId, PaymentOrderRequest request);
    
    /**
     * 处理支付回调
     * 
     * @param callback 支付回调请求
     * @throws RuntimeException 如果回调验证失败
     */
    void handlePaymentCallback(PaymentCallbackRequest callback);
    
    /**
     * 检查用户是否已支付某文档
     * 
     * @param userId 用户ID
     * @param documentId 文档ID
     * @return 是否已支付
     */
    boolean hasPaid(String userId, String documentId);
    
    /**
     * 获取支付状态
     * 
     * @param userId 用户ID
     * @param documentId 文档ID
     * @return 支付状态信息
     */
    PaymentStatusResponse getPaymentStatus(String userId, String documentId);
    
    /**
     * 获取用户的支付记录
     * 
     * @param userId 用户ID
     * @param page 分页参数
     * @return 支付记录列表
     */
    Page<PaymentRecord> getUserPaymentRecords(String userId, Pageable pageable);
    
    /**
     * 获取文档的收入统计
     * 
     * @param documentId 文档ID
     * @return 总收入
     */
    BigDecimal getDocumentRevenue(String documentId);
    
    /**
     * 获取作者的总收入
     * 
     * @param authorId 作者ID
     * @return 总收入
     */
    BigDecimal getAuthorTotalRevenue(String authorId);
    
    /**
     * 申请退款
     * 
     * @param userId 用户ID
     * @param transactionId 交易ID
     * @param reason 退款原因
     * @return 是否成功发起退款
     */
    boolean requestRefund(String userId, String transactionId, String reason);
    
    /**
     * 验证支付回调签名
     * 
     * @param callback 回调数据
     * @return 签名是否有效
     */
    boolean validateCallbackSignature(PaymentCallbackRequest callback);
    
    /**
     * 生成支付订单号
     * 
     * @return 唯一的订单号
     */
    String generateTransactionId();
}