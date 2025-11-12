package com.yunke.backend.security;

import com.yunke.backend.user.domain.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * AFFiNE用户详情
 * 实现Spring Security的UserDetails接口
 * 
 * 注意：权限信息由RoleService动态注入，不在此类中硬编码
 */
public class AffineUserDetails implements UserDetails {

    private final User user;
    
    /**
     * 用户权限列表（由RoleService注入）
     * 注意：不使用@Getter，因为已经有getAuthorities()方法
     */
    private final List<GrantedAuthority> authorities;
    
    /**
     * 构造函数 - 接收用户和权限列表
     * @param user 用户实体
     * @param authorities 权限列表（由RoleService提供）
     */
    public AffineUserDetails(User user, List<GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true; // 可以根据User实体的状态字段来决定
    }

    /**
     * 获取用户ID
     */
    public String getUserId() {
        return user.getId();
    }

    /**
     * 获取用户实体
     */
    public User getUser() {
        return user;
    }
}
