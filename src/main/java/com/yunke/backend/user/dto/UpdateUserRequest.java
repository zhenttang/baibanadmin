package com.yunke.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 更新用户请求
 */
public class UpdateUserRequest {
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotBlank(message = "姓名不能为空")
    @Size(min = 1, max = 50, message = "姓名长度必须在1-50字符之间")
    private String name;
    
    private String avatarUrl;
    
    private Boolean enabled;
    
    private Boolean registered;

    // Constructors
    public UpdateUserRequest() {}

    public UpdateUserRequest(String email, String name) {
        this.email = email;
        this.name = name;
    }

    // Getters and Setters
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public Boolean getRegistered() {
        return registered;
    }

    public void setRegistered(Boolean registered) {
        this.registered = registered;
    }

    @Override
    public String toString() {
        return "UpdateUserRequest{" +
                "email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", enabled=" + enabled +
                ", registered=" + registered +
                '}';
    }
}