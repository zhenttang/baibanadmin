package com.yunke.backend.payment.dto.jeepay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 支付订单查询响应
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayOrderQueryResponse {
    
    /**
     * 支付订单号
     */
    private String payOrderId;
    
    /**
     * 商户订单号
     */
    private String mchOrderNo;
    
    /**
     * 商户号
     */
    private String mchNo;
    
    /**
     * 应用ID
     */
    private String appId;
    
    /**
     * 商品标题
     */
    private String subject;
    
    /**
     * 支付金额(分)
     */
    private Long amount;
    
    /**
     * 货币代码
     */
    private String currency;
    
    /**
     * 订单状态: 0-订单生成, 1-支付中, 2-支付成功, 3-支付失败, 4-已撤销, 5-已退款, 6-订单关闭
     */
    private Integer state;
    
    /**
     * 支付方式代码
     */
    private String wayCode;
    
    /**
     * 支付方式名称
     */
    private String wayName;
    
    /**
     * 支付接口代码
     */
    private String ifCode;
    
    /**
     * 支付接口名称
     */
    private String ifName;
    
    /**
     * 支付成功时间
     */
    private Long successTime;
    
    /**
     * 订单创建时间
     */
    private Long createdAt;
    
    /**
     * 订单更新时间
     */
    private Long updatedAt;
    
    /**
     * 渠道订单号
     */
    private String channelOrderNo;
    
    /**
     * 渠道用户标识
     */
    private String channelUser;
    
    /**
     * 商户扩展参数
     */
    private String extParam;
    
    /**
     * 错误代码
     */
    private String errCode;
    
    /**
     * 错误描述
     */
    private String errMsg;
}