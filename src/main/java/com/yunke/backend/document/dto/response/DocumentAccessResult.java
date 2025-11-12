package com.yunke.backend.document.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 文档访问权限结果
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentAccessResult {

    private boolean hasFullAccess;      // 是否有完整访问权限
    private boolean needFollow;         // 是否需要关注
    private boolean needPurchase;       // 是否需要购买
    private BigDecimal price;           // 价格
    private BigDecimal discountPrice;   // 优惠价
    private int previewLength;          // 预览长度

    public static DocumentAccessResult fullAccess() {
        return new DocumentAccessResult(true, false, false, null, null, 0);
    }

    public static DocumentAccessResult needFollow() {
        return new DocumentAccessResult(false, true, false, null, null, 0);
    }

    public static DocumentAccessResult needPurchase(
            BigDecimal price, BigDecimal discountPrice, int previewLength) {
        return new DocumentAccessResult(
            false, false, true, price, discountPrice, previewLength);
    }
}
