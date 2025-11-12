package com.yunke.backend.controller;

import com.yunke.backend.infrastructure.config.AffineConfig;
import com.yunke.backend.security.dto.AuthResult;
import com.yunke.backend.security.dto.security.LoginCheckResult;
import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.security.AffineUserDetails;
import com.yunke.backend.security.service.AuthService;
import com.yunke.backend.security.service.LoginProtectionService;
import com.yunke.backend.user.service.UserService;
import com.yunke.backend.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.*;

/**
 * 认证控制器
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final JwtUtil jwtUtil;
    private final AffineConfig affineConfig;
    private final LoginProtectionService loginProtection;

    /**
     * 登录预检
     */
    @PostMapping("/preflight")
    public Mono<ResponseEntity<AuthService.PreflightResult>> preflight(@RequestBody PreflightRequest request) {
        log.info("Preflight request for email: {}", request.email());
        
        return authService.preflight(request.email())
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.badRequest().build());
    }

    /**
     * 发送魔法链接
     */
    @PostMapping("/magic-link-send")
    public Mono<ResponseEntity<Map<String, Object>>> sendMagicLink(@RequestBody MagicLinkSendRequest request) {
        log.info("=== Magic Link Send Request ===");
        log.info("Request email: {}", request.email());
        log.info("Request callbackUrl: {}", request.callbackUrl());
        
        return authService.sendMagicLink(request.email(), request.callbackUrl(), request.clientNonce())
                .map(token -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Magic link sent successfully");
                    response.put("email", request.email());
                    // 开发环境下返回 token，生产环境应该通过邮件发送
                    response.put("token", token);
                    
                    log.info("Magic link sent successfully to: {}", request.email());
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    log.error("Failed to send magic link to {}: {}", request.email(), error.getMessage());
                    
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("error", "Failed to send magic link");
                    errorResponse.put("message", error.getMessage());
                    
                    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                });
    }

    /**
     * 魔法链接认证
     */
    @PostMapping("/magic-link")
    public Mono<ResponseEntity<Map<String, Object>>> signInWithMagicLink(@RequestBody MagicLinkSignInRequest request) {
        log.info("=== Magic Link Sign-in Request ===");
        log.info("Request email: {}", request.email());
        log.info("Request token: {}", request.token());
        
        return authService.signInWithMagicLink(request.email(), request.token(), request.clientNonce())
                .map(currentUser -> {
                    log.info("Magic link authentication successful for: {}", currentUser.email());
                    
                    // 生成JWT令牌
                    String accessToken = authService.generateJwtToken(currentUser.id());
                    String refreshToken = authService.generateRefreshToken(currentUser.id());
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    
                    // 构造用户信息
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", currentUser.id());
                    userInfo.put("email", currentUser.email());
                    userInfo.put("name", currentUser.name());
                    userInfo.put("hasPassword", currentUser.hasPassword());
                    userInfo.put("avatarUrl", currentUser.avatarUrl());
                    userInfo.put("emailVerified", currentUser.emailVerified());
                    
                    // 添加令牌到响应
                    response.put("user", userInfo);
                    response.put("token", accessToken);
                    response.put("refreshToken", refreshToken);
                    response.put("expiresIn", 604800); // 7天
                    
                    log.info("Magic link sign-in response prepared for user: {}", currentUser.email());
                    return ResponseEntity.ok(response);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    log.warn("Magic link authentication failed for user: {} - invalid token", request.email());
                    
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("error", "Invalid magic link");
                    errorResponse.put("message", "The magic link is invalid or expired");
                    
                    return Mono.just(ResponseEntity.status(401).body(errorResponse));
                }))
                .onErrorResume(error -> {
                    log.error("Magic link sign-in failed with error: {}", error.getMessage(), error);
                    
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Internal server error");
                    errorResponse.put("success", false);
                    errorResponse.put("message", "An error occurred during magic link authentication");
                    
                    return Mono.just(ResponseEntity.status(500).body(errorResponse));
                });
    }

    /**
     * 发送验证码
     */
    @PostMapping("/send-verification-code")
    public Mono<ResponseEntity<Map<String, Object>>> sendVerificationCode(@RequestBody SendVerificationCodeRequest request) {
        log.info("=== Send Verification Code Request ===");
        log.info("Request email: {}", request.email());
        
        return authService.sendVerificationCode(request.email())
                .map(code -> {
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("message", "Verification code sent successfully");
                    response.put("code", code); // 开发阶段直接返回验证码，生产环境应该通过邮件发送
                    
                    log.info("Verification code generated for {}: {}", request.email(), code);
                    return ResponseEntity.ok(response);
                })
                .onErrorResume(error -> {
                    log.error("Failed to send verification code for {}: {}", request.email(), error.getMessage());
                    
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("error", "Failed to send verification code");
                    errorResponse.put("message", error.getMessage());
                    
                    return Mono.just(ResponseEntity.badRequest().body(errorResponse));
                });
    }

    /**
     * 验证码登录 - 纯JWT认证版本（集成登录保护）
     */
    @PostMapping("/sign-in-with-code")
    public Mono<ResponseEntity<Map<String, Object>>> signInWithCode(
            @RequestBody SignInWithCodeRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        
        log.info("=== Sign-in with Code Request Started ===");
        log.info("Request email: {}", request.email());
        log.info("Request code: {}", request.code());
        
        // 获取客户端IP和User-Agent
        String clientIp = getClientIp(httpRequest);
        String userAgent = httpRequest.getHeader("User-Agent");
        
        // 1. 检查登录保护（是否允许登录）
        LoginCheckResult checkResult = loginProtection.checkLoginAllowed(request.email(), clientIp);
        
        if (checkResult.isBlocked()) {
            log.warn("🚫 登录被阻止 - 用户: {}, IP: {}, 原因: {}", 
                    request.email(), clientIp, checkResult.getMessage());
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Account locked");
            errorResponse.put("message", checkResult.getMessage());
            
            return Mono.just(ResponseEntity.status(403).body(errorResponse));
        }
        
        if (checkResult.isNeedCaptcha()) {
            log.warn("⚠️ 需要验证码 - 用户: {}, IP: {}", request.email(), clientIp);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("error", "Captcha required");
            errorResponse.put("message", checkResult.getMessage());
            errorResponse.put("needCaptcha", true);
            
            return Mono.just(ResponseEntity.status(428).body(errorResponse));  // 428 Precondition Required
        }
        
        // 2. 尝试登录
        return authService.signInWithVerificationCode(request.email(), request.code())
                .map(currentUser -> {
                    log.info("✅ 验证码登录成功 - 用户: {}", currentUser.email());
                    
                    // 登录成功，清除失败记录
                    loginProtection.clearLoginFailures(request.email(), clientIp);
                    
                    // 检测异常登录（新IP）
                    loginProtection.isAnomalousLogin(request.email(), clientIp, userAgent);
                    
                    // 生成JWT令牌
                    String accessToken = authService.generateJwtToken(currentUser.id());
                    String refreshToken = authService.generateRefreshToken(currentUser.id());
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    
                    // 构造用户信息
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", currentUser.id());
                    userInfo.put("email", currentUser.email());
                    userInfo.put("name", currentUser.name());
                    userInfo.put("hasPassword", currentUser.hasPassword());
                    userInfo.put("avatarUrl", currentUser.avatarUrl());
                    userInfo.put("emailVerified", currentUser.emailVerified());
                    
                    // 添加令牌到响应
                    response.put("user", userInfo);
                    response.put("token", accessToken);
                    response.put("refreshToken", refreshToken);
                    response.put("expiresIn", 604800); // 7天
                    
                    log.info("Sign-in with code response prepared for user: {}", currentUser.email());
                    log.info("=== Sign-in with Code Request Completed Successfully ===");
                    return ResponseEntity.ok(response);
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // 处理验证码验证失败的情况
                    log.warn("❌ 验证码登录失败 - 用户: {}, IP: {}", request.email(), clientIp);
                    
                    // 记录登录失败
                    loginProtection.recordLoginFailure(request.email(), clientIp, userAgent);
                    
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("error", "Invalid verification code");
                    errorResponse.put("message", "The verification code is invalid or expired");
                    
                    log.info("=== Sign-in with Code Request Failed - Invalid Code ===");
                    return Mono.just(ResponseEntity.status(401).body(errorResponse));
                }))
                .onErrorResume(error -> {
                    log.error("Sign-in with code failed with error: {}", error.getMessage(), error);
                    
                    // 记录登录失败
                    loginProtection.recordLoginFailure(request.email(), clientIp, userAgent);
                    
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Internal server error");
                    errorResponse.put("success", false);
                    errorResponse.put("message", "An error occurred during verification code authentication");
                    
                    log.info("=== Sign-in with Code Request Failed - Server Error ===");
                    return Mono.just(ResponseEntity.status(500).body(errorResponse));
                });
    }
    
    /**
     * 获取客户端真实IP
     */
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Real-IP");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Forwarded-For");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }

    /**
     * 用户登录
     */
    @PostMapping("/sign-in") 
    public Mono<ResponseEntity<Map<String, Object>>> signIn(
            @RequestBody SignInRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        
        log.info("=== Sign-in Request Started ===");
        log.info("Request email: {}", request.email());
        log.info("Request password present: {}", request.password() != null && !request.password().isEmpty());
        log.info("Request callbackUrl: {}", request.callbackUrl());
        log.info("Request clientNonce: {}", request.clientNonce());
        
        return authService.signIn(request.email(), request.password())
                .flatMap(currentUser -> {
                    log.info("User authentication successful for: {}", currentUser.email());
                    
                    // 生成JWT令牌
                    String accessToken = authService.generateJwtToken(currentUser.id());
                    String refreshToken = authService.generateRefreshToken(currentUser.id());
                    
                    log.info("Generated JWT token for user: {}", currentUser.email());
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    
                    // 构造用户信息
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", currentUser.id());
                    userInfo.put("email", currentUser.email());
                    userInfo.put("name", currentUser.name());
                    userInfo.put("hasPassword", currentUser.hasPassword());
                    userInfo.put("avatarUrl", currentUser.avatarUrl());
                    userInfo.put("emailVerified", currentUser.emailVerified());
                    userInfo.put("features", currentUser.features());
                    
                    // 添加令牌到响应
                    response.put("user", userInfo);
                    response.put("token", accessToken);
                    response.put("refreshToken", refreshToken);
                    response.put("expiresIn", 604800); // 令牌有效期，7天
                    
                    log.info("Sign-in response prepared for user: {}", currentUser.email());
                    log.info("=== Sign-in Request Completed Successfully ===");
                    return Mono.just(ResponseEntity.ok(response));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // 处理登录失败的情况（用户不存在或密码错误）
                    log.warn("Login failed for user: {} - empty response from authService", request.email());
                    
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("success", false);
                    errorResponse.put("error", "Authentication failed");
                    errorResponse.put("message", "Invalid email or password");
                    
                    log.info("=== Sign-in Request Failed - Invalid Credentials ===");
                    return Mono.just(ResponseEntity.status(401).body(errorResponse));
                }))
                .doOnError(error -> {
                    log.error("Sign-in failed with error: {}", error.getMessage(), error);
                })
                .onErrorResume(error -> {
                    Map<String, Object> errorResponse = new HashMap<>();
                    errorResponse.put("error", "Internal server error");
                    errorResponse.put("success", false);
                    errorResponse.put("message", "An error occurred during authentication");
                    
                    log.info("=== Sign-in Request Failed - Server Error ===");
                    return Mono.just(ResponseEntity.status(500).body(errorResponse));
                });
    }

    /**
     * 用户注册
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        log.info("Register request for email: {}", request.email());
        
        AuthResult result = authService.register(request.email(), request.password(), request.name()).block();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Registration successful");
        
        if (result != null) {
            response.put("userId", result.getUserId());
            response.put("email", result.getEmail());
            response.put("name", result.getName());
            response.put("token", result.getToken());
            response.put("refreshToken", result.getRefreshToken());
            response.put("expiresIn", result.getExpiresIn());
            response.put("isNewUser", result.isNewUser());
        }
        
        return ResponseEntity.ok(response);
    }

    /**
     * 刷新令牌
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refreshToken(@RequestBody RefreshTokenRequest request) {
        log.info("Refresh token request received");
        
        try {
            // 验证刷新令牌
            String userId = jwtUtil.validateRefreshToken(request.refreshToken());
            
            if (userId == null) {
                log.warn("Invalid refresh token");
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "Invalid or expired refresh token"
                ));
            }
            
            // 获取用户信息
            Optional<User> userOpt = authService.findUserById(userId);
            if (userOpt.isEmpty()) {
                log.warn("User not found for token: {}", userId);
                return ResponseEntity.status(401).body(Map.of(
                    "success", false,
                    "error", "User not found"
                ));
            }
            
            User user = userOpt.get();
            
            // 生成新令牌
            String newAccessToken = authService.generateJwtToken(userId);
            String newRefreshToken = authService.generateRefreshToken(userId);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", newAccessToken);
            response.put("refreshToken", newRefreshToken);
            response.put("expiresIn", 604800); // 7天
            
            // 添加用户信息
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("id", user.getId());
            userInfo.put("email", user.getEmail());
            userInfo.put("name", user.getName());
            userInfo.put("hasPassword", user.getPassword() != null && !user.getPassword().isEmpty());
            userInfo.put("avatarUrl", user.getAvatarUrl());
            userInfo.put("emailVerified", user.getEmailVerifiedAt() != null);
            
            response.put("user", userInfo);
            
            log.info("Token refreshed successfully for user: {}", userId);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Error refreshing token", e);
            return ResponseEntity.status(401).body(Map.of(
                "success", false,
                "error", "Token refresh failed",
                "message", e.getMessage()
            ));
        }
    }

    /**
     * 获取当前用户信息 - 增强版，包含完整权限信息
     */
    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(
            HttpServletRequest request,
            Authentication authentication) {
        
        log.info("=== 获取当前用户信息 /api/auth/me ===");
        
        // 从Authorization头获取JWT并验证
        String authHeader = request.getHeader("Authorization");
        log.info("Authorization头: {}", authHeader != null ? "存在" : "不存在");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String userId = jwtUtil.validateAccessToken(token);
            
            if (userId != null) {
                // 从数据库获取用户信息
                Optional<User> userOpt = authService.findUserById(userId);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("name", user.getName());
                    userInfo.put("hasPassword", user.getPassword() != null && !user.getPassword().isEmpty());
                    userInfo.put("avatarUrl", user.getAvatarUrl());
                    userInfo.put("emailVerified", user.getEmailVerifiedAt() != null);
                    userInfo.put("registered", user.isRegistered());
                    userInfo.put("enabled", user.isEnabled());
                    
                    // 获取用户features列表，包含admin权限
                    List<String> features = userService.getUserFeatures(userId);
                    userInfo.put("features", features);
                    
                    // 获取详细权限信息
                    Map<String, Object> permissions = new HashMap<>();
                    permissions.put("isAdmin", features.contains("admin"));
                    permissions.put("isSuperAdmin", features.contains("super_admin"));
                    permissions.put("isModerator", features.contains("moderator"));
                    userInfo.put("permissions", permissions);
                    
                    // 获取JWT token信息
                    Map<String, Object> tokenInfo = new HashMap<>();
                    tokenInfo.put("remainingTime", jwtUtil.getTokenRemainingTime(token));
                    tokenInfo.put("needsRefresh", jwtUtil.needsRefresh(token));
                    tokenInfo.put("sessionId", jwtUtil.getSessionIdFromToken(token));
                    userInfo.put("tokenInfo", tokenInfo);
                    
                    Map<String, Object> response = new HashMap<>();
                    response.put("success", true);
                    response.put("user", userInfo);
                    
                    log.info("用户信息获取成功: {}, features: {}", user.getEmail(), features);
                    return ResponseEntity.ok(response);
                } else {
                    log.warn("用户ID {} 对应的用户不存在", userId);
                }
            } else {
                log.warn("JWT token验证失败");
            }
        } else {
            log.warn("缺少Authorization头或格式不正确");
        }
        
        // 如果没有有效的JWT，返回401未认证
        log.info("用户未认证，返回401");
        return ResponseEntity.status(401).body(Map.of(
            "success", false,
            "error", "Unauthorized",
            "message", "Valid authentication token required"
        ));
    }

    /**
     * 获取当前会话信息 - 纯JWT认证版本
     */
    @GetMapping("/session")
    public ResponseEntity<Map<String, Object>> getSession(
            HttpServletRequest request,
            Authentication authentication) {
        
        Map<String, Object> response = new HashMap<>();
        
        // 从Authorization头获取JWT并验证
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String userId = jwtUtil.validateAccessToken(token);
            
            if (userId != null) {
                // 从数据库获取用户信息
                User user = userService.getUserById(userId);
                if (user != null) {
                    Map<String, Object> userInfo = new HashMap<>();
                    userInfo.put("id", user.getId());
                    userInfo.put("email", user.getEmail());
                    userInfo.put("name", user.getName());
                    userInfo.put("hasPassword", user.getPassword() != null);
                    userInfo.put("avatarUrl", user.getAvatarUrl());
                    userInfo.put("emailVerified", user.getEmailVerifiedAt() != null);
                    
                    response.put("user", userInfo);
                    log.info("JWT session found for user: {}", user.getEmail());
                    return ResponseEntity.ok(response);
                }
            }
        }
        
        // 如果没有有效的JWT，返回未认证状态
        response.put("user", null);
        log.info("No valid JWT session found");
        return ResponseEntity.ok(response);
    }

    /**
     * 用户退出登录 - JWT版本
     */
    @GetMapping("/sign-out")
    public ResponseEntity<Map<String, Object>> signOut(
            HttpServletRequest request,
            Authentication authentication) {
        
        log.info("=== Sign-out Request Started ===");
        
        // 从Authorization头获取JWT token
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String userId = jwtUtil.validateAccessToken(token);
            
            if (userId != null) {
                log.info("JWT token found for user: {}, invalidating session", userId);
                // 在JWT系统中，退出登录主要由前端处理（删除存储的token）
                // 这里可以添加token黑名单逻辑，但简单起见直接返回成功
            }
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Sign-out successful");
        
        log.info("=== Sign-out Request Completed Successfully ===");
        return ResponseEntity.ok(response);
    }



    /**
     * 用户登出 - 增强版，支持token撤销
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @RequestBody(required = false) LogoutRequest request,
            HttpServletRequest httpRequest) {
        
        log.info("=== Logout request ===");
        
        // 从Authorization头获取当前token
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            // 撤销当前token
            jwtUtil.revokeToken(token);
            
            // 如果提供了会话ID，也撤销对应的会话
            if (request != null && request.sessionId() != null) {
                authService.logout(request.sessionId());
            }
            
            log.info("Token revoked and user logged out");
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Logout successful");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 撤销所有用户token（强制登出所有设备）
     */
    @PostMapping("/revoke-all")
    public ResponseEntity<Map<String, Object>> revokeAllTokens(HttpServletRequest request) {
        log.info("=== Revoke all tokens request ===");
        
        // 从Authorization头获取JWT并验证
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String userId = jwtUtil.validateAccessToken(token);
            
            if (userId != null) {
                // 撤销用户的所有token
                jwtUtil.revokeAllUserTokens(userId);
                
                Map<String, Object> response = new HashMap<>();
                response.put("success", true);
                response.put("message", "All tokens revoked successfully");
                
                log.info("All tokens revoked for user: {}", userId);
                return ResponseEntity.ok(response);
            }
        }
        
        return ResponseEntity.status(401).body(Map.of(
            "success", false,
            "error", "Unauthorized",
            "message", "Valid authentication token required"
        ));
    }

    /**
     * 用户登出
     */
    @PostMapping("/sign-out")
    public ResponseEntity<Map<String, Object>> signOut(@RequestBody LogoutRequest request) {
        log.info("Logout request for session: {}", request.sessionId());
        
        authService.logout(request.sessionId());
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Logout successful");
        
        return ResponseEntity.ok(response);
    }

    /**
     * 临时接口：为现有用户设置密码（仅用于开发调试）
     */
    @PostMapping("/set-password-for-user")
    public ResponseEntity<Map<String, Object>> setPasswordForUser(@RequestBody SetPasswordRequest request) {
        log.info("=== Set password for user: {} ===", request.email());
        
        try {
            // 查找用户
            Optional<User> userOpt = authService.findUserById(request.email());
            if (userOpt.isEmpty()) {
                // 尝试通过邮箱查找
                userOpt = userService.findByEmail(request.email()).blockOptional();
            }
            
            if (userOpt.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "User not found: " + request.email()
                ));
            }
            
            User user = userOpt.get();
            log.info("Found user: {}, current password is null: {}", user.getEmail(), user.getPassword() == null);
            
            // 更新密码
            userService.updatePassword(user.getId(), request.password());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Password updated successfully for user: " + user.getEmail());
            response.put("userId", user.getId());
            
            log.info("Password updated successfully for user: {}", user.getEmail());
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error setting password for user: {}", request.email(), e);
            return ResponseEntity.status(500).body(Map.of(
                "success", false,
                "error", "Failed to set password",
                "message", e.getMessage()
            ));
        }
    }

    // 请求数据类
    public record PreflightRequest(String email) {}
    public record MagicLinkSendRequest(String email, String callbackUrl, String clientNonce) {}
    public record MagicLinkSignInRequest(String email, String token, String clientNonce) {}
    public record SignInRequest(String email, String password, String callbackUrl, String clientNonce) {}
    public record SendVerificationCodeRequest(String email) {}
    public record SignInWithCodeRequest(String email, String code) {}
    public record RegisterRequest(String email, String password, String name) {}
    public record RefreshTokenRequest(String refreshToken) {}
    public record LogoutRequest(String sessionId) {}
    public record SetPasswordRequest(String email, String password) {}
}