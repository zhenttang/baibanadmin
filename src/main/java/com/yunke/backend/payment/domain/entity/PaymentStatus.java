package com.yunke.backend.payment.domain.entity;

/**
 * 支付状态枚举
 */
public enum PaymentStatus {
    
    /**
     * 待支付
     */
    PENDING("待支付"),
    
    /**
     * 支付中
     */
    PROCESSING("支付中"),
    
    /**
     * 支付成功
     */
    SUCCESS("支付成功"),
    
    /**
     * 支付失败
     */
    FAILED("支付失败"),
    
    /**
     * 已取消
     */
    CANCELLED("已取消"),
    
    /**
     * 已退款
     */
    REFUNDED("已退款"),
    
    /**
     * 已关闭
     */
    CLOSED("已关闭"),
    
    /**
     * 未知状态
     */
    UNKNOWN("未知状态");
    
    private final String description;
    
    PaymentStatus(String description) {
        this.description = description;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * 从Jeepay状态转换为AFFiNE状态
     */
    public static PaymentStatus fromJeepayState(Integer jeepayState) {
        if (jeepayState == null) {
            return UNKNOWN;
        }
        
        return switch (jeepayState) {
            case 0 -> PENDING;      // 订单生成
            case 1 -> PROCESSING;   // 支付中
            case 2 -> SUCCESS;      // 支付成功
            case 3 -> FAILED;       // 支付失败
            case 4 -> CANCELLED;    // 已撤销
            case 5 -> REFUNDED;     // 已退款
            case 6 -> CLOSED;       // 订单关闭
            default -> UNKNOWN;
        };
    }
}