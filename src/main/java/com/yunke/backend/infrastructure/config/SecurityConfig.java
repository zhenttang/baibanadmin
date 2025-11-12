package com.yunke.backend.infrastructure.config;

import com.yunke.backend.security.JwtAuthenticationFilter;
import com.yunke.backend.security.JwtAuthenticationProvider;
import com.yunke.backend.security.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * Spring Security 配置
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtAuthenticationProvider jwtAuthenticationProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;


    /**
     * 认证管理器
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }


    /**
     * 安全过滤器链
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        log.info("配置 Security 过滤器链 - 启用基本权限控制");
        
        http
            // 禁用CSRF
            .csrf(AbstractHttpConfigurer::disable)
            
            // 配置CORS
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            
            // 配置会话管理
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            
            // 配置权限控制
            .authorizeHttpRequests(auth -> auth
                // 允许认证相关接口
                .requestMatchers("/api/auth/**").permitAll()
                
                // 允许健康检查接口
                .requestMatchers("/api/health/**", "/actuator/**").permitAll()
                
                // 允许静态资源和上传文件访问
                .requestMatchers("/static/**", "/public/**", "/uploads/**").permitAll()
                
                // 允许worker接口（图片代理等工具类接口）
                .requestMatchers("/api/worker/**").permitAll()
                
                // admin接口需要管理员权限
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                
                // 暂时开放 workspaces API 用于测试
                .requestMatchers("/api/workspaces/**").permitAll()
                
                // 暂时开放 copilot API 用于测试（修复403错误）
                .requestMatchers("/api/copilot/**").permitAll()
                
                // 暂时开放 users/me API 用于测试（修复403错误）
                .requestMatchers("/api/users/me/**").permitAll()

                // 开放社区浏览API（允许匿名访问公开内容）
                .requestMatchers("/api/community/documents/**").permitAll()
                .requestMatchers("/api/community/categories/**").permitAll()
                .requestMatchers("/api/community/tags/**").permitAll()
                .requestMatchers("/api/community/authors/**").permitAll()

                // 其他API接口需要认证
                .requestMatchers("/api/**").authenticated()
                
                // 其他请求放行
                .anyRequest().permitAll()
            )
            
            // 添加JWT过滤器
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
            
            // 配置认证提供者
            .authenticationProvider(jwtAuthenticationProvider)
            
            // 配置权限拒绝处理器
            .exceptionHandling(exceptions -> exceptions
                .accessDeniedHandler(accessDeniedHandler())
            );

        return http.build();
    }
    
    /**
     * 权限拒绝处理器 - 记录详细的权限检查失败信息
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (HttpServletRequest request, HttpServletResponse response, 
                AccessDeniedException accessDeniedException) -> {
            
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String requestURI = request.getRequestURI();
            String method = request.getMethod();
            
            if (auth != null) {
                log.error("❌ 权限拒绝 - 用户: {}, 请求: {} {}, 所需角色: ADMIN, 用户权限: {}", 
                        auth.getName(), method, requestURI, auth.getAuthorities());
            } else {
                log.error("❌ 权限拒绝 - 未认证用户, 请求: {} {}", method, requestURI);
            }
            
            // 返回403错误
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":\"FORBIDDEN\",\"message\":\"您没有权限执行此操作\"}");
        };
    }

    /**
     * CORS配置
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 使用 AllowedOriginPatterns 支持所有 localhost 和 127.0.0.1 端口
        configuration.setAllowedOriginPatterns(List.of(
                // Android Capacitor 应用支持（关键！）
                "http://localhost",
                "capacitor://localhost",
                // 开发环境 - 支持所有端口
                "http://localhost:*",
                "http://127.0.0.1:*",
                "http://172.24.48.1:*",
                // Android开发环境 - 支持本地IP所有端口
                "http://192.168.2.4:*",
                // 生产域名
                "http://c.yckeji0316.cn",
                "http://f.yckeji0316.cn",
                "http://b.yckeji0316.cn",
                "https://b.yckeji0316.cn",
                "http://ykweb.yckeji0316.cn",
                "https://ykweb.yckeji0316.cn",
                "http://ykmodile.yckeji0316.cn",
                "https://ykmodile.yckeji0316.cn",
                "http://ykadmin.yckeji0316.cn:8080",
                "https://ykadmin.yckeji0316.cn:8080",
                "http://ykbaiban.yckeji0316.cn",
                "https://ykbaiban.yckeji0316.cn"
        ));

        // 允许的方法
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "HEAD"));

        // 允许的头部
        configuration.setAllowedHeaders(List.of("*"));

        // 允许暴露的响应头
        configuration.setExposedHeaders(List.of("Authorization", "Content-Type", "X-Requested-With", "Accept",
                                              "Origin", "Access-Control-Request-Method", "Access-Control-Request-Headers"));

        // 允许凭证 - 支持JWT和Cookie认证
        configuration.setAllowCredentials(true);

        // 预检请求缓存时间增加
        configuration.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        
        return source;
    }
}
