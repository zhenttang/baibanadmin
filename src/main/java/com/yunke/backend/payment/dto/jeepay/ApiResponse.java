package com.yunke.backend.payment.dto.jeepay;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Jeepay API响应基础类
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
    /**
     * 响应代码 0-成功
     */
    private Integer code;

    /**
     * 响应消息
     */
    private String msg;

    /**
     * 响应数据
     */
    private T data;

    /**
     * 签名
     */
    private String sign;

    /**
     * 请求是否成功
     */
    public boolean isSuccess() {
        return code != null && code == 0;
    }
}

