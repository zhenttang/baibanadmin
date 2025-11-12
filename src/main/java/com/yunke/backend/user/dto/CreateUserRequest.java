package com.yunke.backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 创建用户请求
 */
public class CreateUserRequest {
    
    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    private String email;
    
    @NotBlank(message = "姓名不能为空")
    @Size(min = 1, max = 50, message = "姓名长度必须在1-50字符之间")
    private String name;
    
    @Size(min = 6, max = 50, message = "密码长度必须在6-50字符之间")
    private String password;
    
    private String avatarUrl;
    
    private Boolean enabled;

    // Constructors
    public CreateUserRequest() {}

    public CreateUserRequest(String email, String name, String password) {
        this.email = email;
        this.name = name;
        this.password = password;
        this.enabled = true;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    @Override
    public String toString() {
        return "CreateUserRequest{" +
                "email='" + email + '\'' +
                ", name='" + name + '\'' +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", enabled=" + enabled +
                '}';
    }
}