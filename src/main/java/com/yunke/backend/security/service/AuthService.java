package com.yunke.backend.security.service;

import com.yunke.backend.security.dto.AuthResult;
import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.user.domain.entity.UserSession;
import com.yunke.backend.system.domain.entity.ConnectedAccount;
import reactor.core.publisher.Mono;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.List;
import java.util.Optional;

/**
 * 认证服务接口
 */
public interface AuthService {

    /**
     * 登录
     */
    Mono<AuthResult> login(String email, String password);

    /**
     * 注册
     */
    Mono<AuthResult> register(String email, String password, String name);

    /**
     * 刷新令牌
     */
    Mono<AuthResult> refreshToken(String refreshToken);

    /**
     * 登出
     */
    Mono<Boolean> logout(String refreshToken);

    /**
     * 发送密码重置邮件
     */
    Mono<Boolean> sendPasswordResetEmail(String email);

    /**
     * 重置密码
     */
    Mono<Boolean> resetPassword(String token, String newPassword);

    /**
     * 验证令牌
     */
    Mono<Boolean> validateToken(String token);
    
    /**
     * 生成JWT访问令牌
     */
    String generateJwtToken(String userId);
    
    /**
     * 生成JWT刷新令牌
     */
    String generateRefreshToken(String userId);
    
    /**
     * 根据ID查找用户
     */
    Optional<User> findUserById(String userId);

    /**
     * 检查用户登录前置信息
     */
    Mono<PreflightResult> preflight(String email);

    /**
     * 用户密码登录
     */
    Mono<CurrentUser> signIn(String email, String password);

    /**
     * 发送验证码
     */
    Mono<String> sendVerificationCode(String email);

    /**
     * 验证码登录
     */
    Mono<CurrentUser> signInWithVerificationCode(String email, String code);

    /**
     * 发送魔法链接
     */
    Mono<String> sendMagicLink(String email, String callbackUrl, String clientNonce);

    /**
     * 魔法链接登录
     */
    Mono<CurrentUser> signInWithMagicLink(String email, String token, String clientNonce);

    /**
     * 用户注册（仅测试环境）
     */
    Mono<CurrentUser> signUp(String email, String password, String name);

    /**
     * 用户登出
     */
    Mono<Void> signOut(String sessionId, String userId);

    /**
     * 撤销用户所有会话
     */
    Mono<Void> revokeUserSessions(String userId);

    /**
     * 检查用户是否可以登录（早期访问控制）
     */
    Mono<Boolean> canSignIn(String email);

    // Session管理
    /**
     * 获取用户会话
     */
    Mono<SessionWithUser> getUserSession(String sessionId, String userId);

    /**
     * 创建用户会话
     */
    Mono<UserSession> createUserSession(String userId, String sessionId, Long ttl);

    /**
     * 刷新用户会话（如果需要）
     */
    Mono<Boolean> refreshUserSessionIfNeeded(HttpServletResponse response, UserSession userSession, Long ttr);

    // Cookie管理
    /**
     * 设置认证Cookie
     */
    Mono<Void> setCookies(HttpServletRequest request, HttpServletResponse response, String userId);

    /**
     * 刷新Cookie
     */
    Mono<Void> refreshCookies(HttpServletResponse response, String sessionId);

    /**
     * 设置用户Cookie
     */
    void setUserCookie(HttpServletResponse response, String userId);

    // 邮箱验证和密码重置
    /**
     * 发送修改密码邮件
     */
    Mono<Boolean> sendChangePasswordEmail(String userId, String callbackUrl);

    /**
     * 发送设置密码邮件
     */
    Mono<Boolean> sendSetPasswordEmail(String userId, String callbackUrl);

    /**
     * 修改密码
     */
    Mono<Boolean> changePassword(String token, String newPassword, String userId);

    /**
     * 发送修改邮箱邮件
     */
    Mono<Boolean> sendChangeEmail(String userId, String callbackUrl);

    /**
     * 发送验证新邮箱邮件
     */
    Mono<Boolean> sendVerifyChangeEmail(String token, String email, String callbackUrl);

    /**
     * 修改邮箱
     */
    Mono<User> changeEmail(String token, String email);

    /**
     * 发送邮箱验证邮件
     */
    Mono<Boolean> sendVerifyEmail(String userId, String callbackUrl);

    /**
     * 验证邮箱
     */
    Mono<Boolean> verifyEmail(String token);

    // OAuth相关
    /**
     * OAuth登录预检
     */
    Mono<OAuthPreflightResult> oauthPreflight(String provider, String redirectUri, String client, String clientNonce);

    /**
     * OAuth回调处理
     */
    Mono<CurrentUser> oauthCallback(String code, String state, String clientNonce);

    /**
     * 获取OAuth连接账户
     */
    Mono<ConnectedAccount> getConnectedAccount(String provider, String providerAccountId);

    /**
     * 创建OAuth连接账户
     */
    Mono<ConnectedAccount> createConnectedAccount(CreateConnectedAccountInput data);

    // 会话查询
    /**
     * 获取当前会话中的所有用户
     */
    Mono<List<CurrentUser>> getSessionUsers(String sessionId);

    /**
     * 获取当前用户信息
     */
    Mono<CurrentUser> getCurrentUser(String sessionId, String userId);

    // 数据传输对象
    /**
     * 当前用户信息
     */
    record CurrentUser(
            String id,
            String email,
            String avatarUrl,
            String name,
            boolean disabled,
            Boolean hasPassword,
            boolean emailVerified,
            List<String> features
    ) {}

    /**
     * 带用户信息的会话
     */
    record SessionWithUser(
            UserSession session,
            CurrentUser user
    ) {}

    /**
     * 登录前置检查结果
     */
    record PreflightResult(
            boolean registered,
            boolean hasPassword
    ) {}

    /**
     * OAuth预检结果
     */
    record OAuthPreflightResult(
            String url
    ) {}

    /**
     * 创建OAuth连接账户输入
     */
    record CreateConnectedAccountInput(
            String userId,
            String provider,
            String providerAccountId,
            String accessToken,
            String refreshToken,
            String idToken,
            String tokenType,
            String scope,
            Long expiresIn
    ) {}
}