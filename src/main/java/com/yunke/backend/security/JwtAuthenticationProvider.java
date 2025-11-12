package com.yunke.backend.security;

import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.security.service.AuthService;
import com.yunke.backend.security.service.permission.PermissionChecker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

/**
 * JWT认证提供者
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationProvider implements AuthenticationProvider {

    private final AuthService authService;
    private final PermissionChecker permissionChecker;

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        String token = (String) authentication.getCredentials();
        
        if (token == null || token.isEmpty()) {
            throw new BadCredentialsException("JWT token is required");
        }
        
        try {
            // 验证JWT令牌
            boolean isValid = authService.validateToken(token).block();
            
            if (!isValid) {
                throw new BadCredentialsException("Invalid JWT token");
            }
            
            // 从令牌中提取用户ID
            String userId = extractUserIdFromToken(token);
            
            // 查询用户
            Optional<User> userOpt = authService.findUserById(userId);
            
            if (userOpt.isEmpty()) {
                throw new BadCredentialsException("User not found");
            }
            
            User user = userOpt.get();
            
            // 从PermissionChecker获取用户权限（已集成缓存）
            List<GrantedAuthority> authorities = permissionChecker.getUserAuthorities(userId);
            
            // 创建UserDetails，注入权限信息
            UserDetails userDetails = new AffineUserDetails(user, authorities);
            
            return new UsernamePasswordAuthenticationToken(
                userDetails, 
                token, 
                userDetails.getAuthorities()
            );
            
        } catch (Exception e) {
            log.debug("JWT authentication failed", e);
            throw new BadCredentialsException("JWT authentication failed", e);
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
    
    /**
     * 从令牌中提取用户ID
     * 注意：这里简化处理，实际应从JWT解析
     */
    private String extractUserIdFromToken(String token) {
        // 这里需要实现从JWT中提取用户ID的逻辑
        // 暂时返回模拟值
        return "user123";
    }
}