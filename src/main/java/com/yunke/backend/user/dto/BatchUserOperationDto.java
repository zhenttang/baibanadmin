package com.yunke.backend.user.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.Builder;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 批量用户操作请求DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUserOperationDto {
    
    /**
     * 用户ID列表
     */
    @NotEmpty(message = "用户ID列表不能为空")
    private List<String> userIds;
    
    /**
     * 操作类型
     */
    public enum OperationType {
        ENABLE,     // 启用
        DISABLE,    // 禁用
        DELETE,     // 删除
        RESET_PASSWORD, // 重置密码
        VERIFY_EMAIL,   // 验证邮箱
        GRANT_ADMIN,    // 授予管理员权限
        REVOKE_ADMIN,   // 撤销管理员权限
        ADD_FEATURE,    // 添加特性
        REMOVE_FEATURE  // 移除特性
    }
    
    /**
     * 操作类型
     */
    private OperationType operation;
    
    /**
     * 操作参数（可选，用于某些需要额外参数的操作）
     */
    private String parameter;
    
    /**
     * 操作原因/备注
     */
    private String reason;
}