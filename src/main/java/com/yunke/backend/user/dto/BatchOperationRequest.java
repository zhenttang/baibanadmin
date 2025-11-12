package com.yunke.backend.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;

/**
 * 批量操作请求
 * 用于管理员批量操作用户（启用、禁用、删除）
 */
public class BatchOperationRequest {
    
    @NotEmpty(message = "用户ID列表不能为空")
    private List<String> userIds;
    
    @NotNull(message = "操作类型不能为空")
    @Pattern(regexp = "enable|disable|delete", message = "操作类型必须是 enable、disable 或 delete")
    private String operation;

    // Constructors
    public BatchOperationRequest() {}

    public BatchOperationRequest(List<String> userIds, String operation) {
        this.userIds = userIds;
        this.operation = operation;
    }

    // Getters and Setters
    public List<String> getUserIds() {
        return userIds;
    }

    public void setUserIds(List<String> userIds) {
        this.userIds = userIds;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    @Override
    public String toString() {
        return "BatchOperationRequest{" +
                "userIds=" + userIds +
                ", operation='" + operation + '\'' +
                '}';
    }
}

