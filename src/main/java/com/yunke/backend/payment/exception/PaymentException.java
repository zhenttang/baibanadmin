package com.yunke.backend.payment.exception;

/**
 * 支付相关异常
 */
public class PaymentException extends RuntimeException {
    
    public PaymentException(String message) {
        super(message);
    }
    
    public PaymentException(String message, Throwable cause) {
        super(message, cause);
    }
}