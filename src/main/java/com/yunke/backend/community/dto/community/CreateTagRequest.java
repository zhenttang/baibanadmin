package com.yunke.backend.community.dto.community;

import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 创建标签请求DTO
 */
@Data
public class CreateTagRequest {
    
    @NotBlank(message = "标签名称不能为空")
    @Size(max = 30, message = "标签名称长度不能超过30个字符")
    private String name;
    
    @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "颜色格式不正确，应为#RRGGBB格式")
    private String color = "#666666";
}