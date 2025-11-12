package com.yunke.backend.security;

import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.security.service.AuthService;
import com.yunke.backend.security.service.permission.PermissionChecker;
import com.yunke.backend.security.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * JWT认证过滤器
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final AuthService authService;
    private final PermissionChecker permissionChecker;
    private final JwtUtil jwtUtil;
    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String HEADER_NAME = "Authorization";
    private static final String SESSION_COOKIE_NAME = "affine_session";
    private static final String USER_COOKIE_NAME = "affine_user";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                  FilterChain filterChain) throws ServletException, IOException {
        
        String requestURI = request.getRequestURI();
        String method = request.getMethod();
        log.debug("=== JWT认证过滤器开始 === {} {}", method, requestURI);
        
        // 跳过不需要JWT认证的路径
        if (shouldSkipFilter(requestURI)) {
            log.debug("跳过JWT认证，路径: {}", requestURI);
            filterChain.doFilter(request, response);
            return;
        }
        
        // 记录所有请求头（仅在DEBUG级别输出）
        if (log.isDebugEnabled()) {
            log.debug("请求头信息:");
            request.getHeaderNames().asIterator().forEachRemaining(headerName -> 
                log.debug("  {}: {}", headerName, request.getHeader(headerName))
            );
        }
        
        // 记录所有cookies（仅在DEBUG级别输出）
        if (log.isDebugEnabled()) {
            Cookie[] cookies = request.getCookies();
            if (cookies != null) {
                log.debug("请求Cookies:");
                Arrays.stream(cookies).forEach(cookie -> 
                    log.debug("  {}: {}", cookie.getName(), cookie.getValue())
                );
            } else {
                log.debug("请求中没有Cookies");
            }
        }
        
        try {
            // 获取JWT令牌
            String token = extractTokenFromRequest(request);
            log.info("🔍 JWT认证过滤器 - 请求URI: {}, Token存在: {}", requestURI, token != null);
            String userId = null;
            
            if (token != null) {
                // 从令牌中提取用户ID并验证
                try {
                    userId = jwtUtil.validateAccessToken(token);
                    log.info("✅ JWT token验证成功，userId: {}", userId);
                } catch (Exception e) {
                    log.warn("❌ JWT token验证失败: {}", e.getMessage());
                    // 测试模式下继续处理请求，不中断认证
                }
            } else {
                log.warn("⚠️  请求中未找到JWT token (检查Authorization头)");
            }
            
            // 如果没有从JWT获取到有效的用户ID，则尝试从Cookie中读取
            if (userId == null && SecurityContextHolder.getContext().getAuthentication() == null) {
                // 尝试从Cookie中读取会话信息
                String sessionId = extractSessionIdFromCookies(request);
                String userIdFromCookie = extractUserIdFromCookies(request);
                
                log.debug("从Cookie提取信息 - sessionId: {}, userId: {}", sessionId, userIdFromCookie);
                
                if (sessionId != null && userIdFromCookie != null) {
                    log.debug("Found session from cookies: {}, userId: {}", sessionId, userIdFromCookie);
                    userId = userIdFromCookie;
                }
            }
            
            // 检查当前SecurityContext状态
            boolean hasExistingAuth = SecurityContextHolder.getContext().getAuthentication() != null;
            log.debug("当前SecurityContext中已有认证信息: {}", hasExistingAuth);
            
            // 如果获取到用户ID且当前上下文中没有认证信息
            if (userId != null && !hasExistingAuth) {
                Optional<User> userOpt = authService.findUserById(userId);
                
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    
                    // 从PermissionChecker获取用户权限（已集成缓存）
                    List<GrantedAuthority> authorities = permissionChecker.getUserAuthorities(userId);
                    
                    // 创建UserDetails，注入权限信息
                    AffineUserDetails userDetails = new AffineUserDetails(user, authorities);
                    
                    // 创建认证对象
                    UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.getAuthorities());
                    
                    // 设置认证详情
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    
                    // 更新安全上下文
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                    
                    log.info("✅ JWT认证成功 - 用户ID: {}, 邮箱: {}, 权限数量: {}, 权限列表: {}", 
                            userId, user.getEmail(), authorities.size(), authorities);
                } else {
                    log.warn("❌ 找不到用户ID对应的用户: {}", userId);
                }
            } else {
                // 没有有效的JWT token，不设置认证信息
                if (userId == null) {
                    log.warn("⚠️  未从请求中提取到有效的用户ID（Token可能无效或缺失）");
                } else {
                    log.debug("当前SecurityContext已有认证信息，跳过重新认证");
                }
            }
        } catch (Exception e) {
            log.error("认证过程中发生异常: {}", e.getMessage());
            // 发生异常时不设置认证信息
        }
        
        // 记录处理前的SecurityContext状态（仅DEBUG级别）
        if (log.isDebugEnabled()) {
            boolean hasAuthBeforeChain = SecurityContextHolder.getContext().getAuthentication() != null;
            log.debug("过滤器链处理前，SecurityContext认证状态: {}", hasAuthBeforeChain);
        }
        
        // 继续处理请求
        filterChain.doFilter(request, response);
        
        // 记录处理后的SecurityContext状态和响应状态（仅DEBUG级别）
        if (log.isDebugEnabled()) {
            boolean hasAuthAfterChain = SecurityContextHolder.getContext().getAuthentication() != null;
            log.debug("过滤器链处理后，SecurityContext认证状态: {}, 响应状态码: {}", hasAuthAfterChain, response.getStatus());
            log.debug("=== JWT认证过滤器结束 === {} {}", method, requestURI);
        }
    }

    /**
     * 从请求中提取JWT令牌
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader(HEADER_NAME);
        
        if (bearerToken != null && bearerToken.startsWith(TOKEN_PREFIX)) {
            return bearerToken.substring(TOKEN_PREFIX.length());
        }
        
        return null;
    }
    
    /**
     * 从Cookie中提取会话ID
     */
    private String extractSessionIdFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }
    
    /**
     * 从Cookie中提取用户ID
     */
    private String extractUserIdFromCookies(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (USER_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }
    
    /**
     * 判断是否应该跳过JWT认证过滤器
     * @param requestURI 请求URI
     * @return true表示跳过，false表示需要处理
     */
    private boolean shouldSkipFilter(String requestURI) {
        // 跳过认证相关的接口
        if (requestURI.startsWith("/api/auth/")) {
            return true;
        }
        
        // 跳过健康检查接口
        if (requestURI.startsWith("/api/health/") || requestURI.startsWith("/actuator/")) {
            return true;
        }
        
        // 跳过 Copilot API - 修复JWT拦截问题
        if (requestURI.startsWith("/api/copilot/")) {
            return true;
        }
        
        // 跳过静态资源
        if (requestURI.startsWith("/static/") || requestURI.startsWith("/public/")) {
            return true;
        }
        
        return false;
    }
}