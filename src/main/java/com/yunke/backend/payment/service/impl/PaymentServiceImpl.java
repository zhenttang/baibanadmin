package com.yunke.backend.payment.service.impl;

import com.yunke.backend.payment.dto.request.PaymentCallbackRequest;
import com.yunke.backend.payment.dto.request.PaymentOrderRequest;
import com.yunke.backend.payment.dto.response.PaymentOrderResponse;
import com.yunke.backend.payment.dto.response.PaymentStatusResponse;
import com.yunke.backend.payment.domain.entity.PaymentRecord;
import com.yunke.backend.payment.repository.PaymentRecordRepository;
import com.yunke.backend.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 支付服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentServiceImpl implements PaymentService {
    
    private final PaymentRecordRepository paymentRecordRepository;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public PaymentOrderResponse createPaymentOrder(String userId, PaymentOrderRequest request) {
        log.info("用户 {} 创建支付订单，文档 {}", userId, request.getDocumentId());
        
        // 检查用户是否已购买
        if (paymentRecordRepository.hasUserPurchasedDocument(userId, request.getDocumentId())) {
            throw new RuntimeException("用户已购买该文档");
        }
        
        // 创建支付记录
        PaymentRecord paymentRecord = new PaymentRecord();
        paymentRecord.setUserId(userId);
        paymentRecord.setDocumentId(request.getDocumentId());
        paymentRecord.setAmount(request.getAmount());
        paymentRecord.setPaymentMethod(request.getPaymentMethod());
        paymentRecord.setTransactionId(generateTransactionId());
        paymentRecord.setStatus(PaymentRecord.PaymentStatus.PENDING);
        paymentRecord.setCreatedAt(LocalDateTime.now());
        paymentRecord.setUpdatedAt(LocalDateTime.now());
        
        paymentRecordRepository.save(paymentRecord);
        
        // 构建支付订单响应
        PaymentOrderResponse response = new PaymentOrderResponse();
        response.setOrderId(Long.valueOf(paymentRecord.getId())); // 转换为Long类型
        response.setTransactionId(paymentRecord.getTransactionId());
        response.setAmount(request.getAmount());
        response.setPaymentMethod(request.getPaymentMethod());
        response.setPaymentUrl("https://pay.example.com/checkout/" + paymentRecord.getTransactionId());
        response.setStatus("PENDING");
        response.setExpireTime(System.currentTimeMillis() + 30 * 60 * 1000); // 30分钟过期
        
        log.info("支付订单创建成功，订单号: {}", response.getOrderId());
        return response;
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void handlePaymentCallback(PaymentCallbackRequest request) {
        log.info("处理支付回调，订单号: {}", request.getTransactionId());
        
        PaymentRecord paymentRecord = paymentRecordRepository.findByTransactionId(request.getTransactionId());
        if (paymentRecord == null) {
            log.warn("支付记录不存在，订单号: {}", request.getTransactionId());
            throw new RuntimeException("支付记录不存在");
        }
        
        // 更新支付状态
        PaymentRecord.PaymentStatus newStatus;
        switch (request.getStatus().toUpperCase()) {
            case "SUCCESS":
                newStatus = PaymentRecord.PaymentStatus.SUCCESS;
                break;
            case "FAILED":
                newStatus = PaymentRecord.PaymentStatus.FAILED;
                break;
            case "REFUNDED":
                newStatus = PaymentRecord.PaymentStatus.REFUNDED;
                break;
            default:
                log.warn("未知的支付状态: {}", request.getStatus());
                return;
        }
        
        paymentRecord.setStatus(newStatus);
        paymentRecord.setUpdatedAt(LocalDateTime.now());
        paymentRecordRepository.save(paymentRecord);
        
        log.info("支付状态更新成功，订单号: {}，状态: {}", request.getTransactionId(), newStatus);
    }
    
    @Override
    public PaymentStatusResponse getPaymentStatus(String userId, String documentId) {
        PaymentRecord paymentRecord = paymentRecordRepository.findByUserIdAndDocumentId(userId, documentId);
        if (paymentRecord == null) {
            PaymentStatusResponse response = new PaymentStatusResponse();
            response.setHasPaid(false);
            response.setStatus("NOT_PAID");
            return response;
        }
        
        PaymentStatusResponse response = new PaymentStatusResponse();
        response.setHasPaid(paymentRecord.getStatus() == PaymentRecord.PaymentStatus.SUCCESS);
        response.setTransactionId(paymentRecord.getTransactionId());
        response.setStatus(paymentRecord.getStatus().name());
        response.setAmount(paymentRecord.getAmount().toString());
        response.setPaymentMethod(paymentRecord.getPaymentMethod());
        response.setPaymentTime(paymentRecord.getCreatedAt().toString());
        
        return response;
    }
    
    @Override
    public boolean hasPaid(String userId, String documentId) {
        return paymentRecordRepository.hasUserPurchasedDocument(userId, documentId);
    }
    
    @Override
    public Page<PaymentRecord> getUserPaymentRecords(String userId, Pageable pageable) {
        return paymentRecordRepository.findByUserIdAndStatus(userId, PaymentRecord.PaymentStatus.SUCCESS, pageable);
    }
    
    @Override
    public BigDecimal getDocumentRevenue(String documentId) {
        return paymentRecordRepository.sumAmountByDocumentId(documentId);
    }
    
    @Override
    public BigDecimal getAuthorTotalRevenue(String authorId) {
        return paymentRecordRepository.sumAmountByAuthorId(authorId);
    }
    
    @Override
    public boolean requestRefund(String userId, String transactionId, String reason) {
        log.info("用户 {} 申请退款，交易ID: {}，原因: {}", userId, transactionId, reason);
        // 这里应该调用第三方支付平台的退款接口
        // 暂时返回true表示退款申请成功
        return true;
    }
    
    @Override
    public boolean validateCallbackSignature(PaymentCallbackRequest callback) {
        // 这里应该验证回调签名
        // 暂时返回true表示验证通过
        return true;
    }
    
    @Override
    public String generateTransactionId() {
        return "PAY" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}