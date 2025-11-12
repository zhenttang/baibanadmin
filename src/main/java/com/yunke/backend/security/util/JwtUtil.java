package com.yunke.backend.security.util;

import com.yunke.backend.infrastructure.config.AuthConfig;
import com.yunke.backend.security.service.JwtBlacklistService;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.UUID;

/**
 * JWT工具类 - 增强版
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtUtil {

    private final AuthConfig authConfig;
    private final JwtBlacklistService jwtBlacklistService;

    /**
     * 生成访问令牌
     */
    public String generateAccessToken(String userId) {
        return generateAccessToken(userId, null);
    }
    
    /**
     * 生成访问令牌（带会话ID）
     */
    public String generateAccessToken(String userId, String sessionId) {
        AuthConfig.Jwt jwtConfig = authConfig.getJwt();
        
        Algorithm algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
        String jti = UUID.randomUUID().toString();
        
        var builder = JWT.create()
                .withIssuer(jwtConfig.getIssuer())
                .withSubject(userId)
                .withJWTId(jti)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + jwtConfig.getExpiration()))
                .withClaim("type", "access");
                
        if (sessionId != null) {
            builder.withClaim("sessionId", sessionId);
        }
        
        String token = builder.sign(algorithm);
        
        // 记录token与用户的关联关系
        if (jwtBlacklistService instanceof com.yunke.backend.service.impl.RedisJwtBlacklistServiceImpl) {
            ((com.yunke.backend.service.impl.RedisJwtBlacklistServiceImpl) jwtBlacklistService)
                .associateUserToken(userId, jti, jwtConfig.getExpiration() / 1000);
        }
        
        return token;
    }

    /**
     * 生成刷新令牌
     */
    public String generateRefreshToken(String userId) {
        return generateRefreshToken(userId, null);
    }
    
    /**
     * 生成刷新令牌（带会话ID）
     */
    public String generateRefreshToken(String userId, String sessionId) {
        AuthConfig.Jwt jwtConfig = authConfig.getJwt();
        
        Algorithm algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
        String jti = UUID.randomUUID().toString();
        
        var builder = JWT.create()
                .withIssuer(jwtConfig.getIssuer())
                .withSubject(userId)
                .withJWTId(jti)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + jwtConfig.getRefreshExpiration()))
                .withClaim("type", "refresh");
                
        if (sessionId != null) {
            builder.withClaim("sessionId", sessionId);
        }
        
        return builder.sign(algorithm);
    }

    /**
     * 验证访问令牌
     */
    public String validateAccessToken(String token) {
        return validateToken(token, "access");
    }

    /**
     * 验证刷新令牌
     */
    public String validateRefreshToken(String token) {
        return validateToken(token, "refresh");
    }
    
    /**
     * 通用令牌验证方法
     */
    private String validateToken(String token, String expectedType) {
        try {
            AuthConfig.Jwt jwtConfig = authConfig.getJwt();
            Algorithm algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
            
            JWTVerifier verifier = JWT.require(algorithm)
                    .withIssuer(jwtConfig.getIssuer())
                    .withClaim("type", expectedType)
                    .build();
            
            DecodedJWT jwt = verifier.verify(token);
            
            // 检查黑名单
            if (jwtConfig.isEnableBlacklist() && jwtBlacklistService.isBlacklisted(jwt.getId())) {
                log.debug("Token {} is blacklisted", jwt.getId());
                return null;
            }
            
            return jwt.getSubject();
        } catch (JWTVerificationException e) {
            log.debug("{} token validation failed: {}", expectedType, e.getMessage());
            return null;
        }
    }

    /**
     * 从令牌中获取用户ID
     */
    public String getUserIdFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getSubject();
        } catch (Exception e) {
            log.debug("Failed to decode token", e);
            return null;
        }
    }
    
    /**
     * 从令牌中获取JTI
     */
    public String getJtiFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getId();
        } catch (Exception e) {
            log.debug("Failed to get JTI from token", e);
            return null;
        }
    }
    
    /**
     * 从令牌中获取会话ID
     */
    public String getSessionIdFromToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getClaim("sessionId").asString();
        } catch (Exception e) {
            log.debug("Failed to get session ID from token", e);
            return null;
        }
    }

    /**
     * 检查令牌是否过期
     */
    public boolean isTokenExpired(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getExpiresAt().before(new Date());
        } catch (Exception e) {
            log.debug("Failed to check token expiration", e);
            return true;
        }
    }
    
    /**
     * 检查令牌是否需要刷新
     */
    public boolean needsRefresh(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            long remainingTime = jwt.getExpiresAt().getTime() - System.currentTimeMillis();
            return remainingTime < authConfig.getJwt().getAutoRefreshThreshold();
        } catch (Exception e) {
            log.debug("Failed to check token refresh need", e);
            return true;
        }
    }

    /**
     * 获取令牌过期时间
     */
    public Instant getTokenExpiration(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return jwt.getExpiresAt().toInstant();
        } catch (Exception e) {
            log.debug("Failed to get token expiration", e);
            return Instant.now();
        }
    }
    
    /**
     * 获取令牌剩余有效时间（秒）
     */
    public long getTokenRemainingTime(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            return (jwt.getExpiresAt().getTime() - System.currentTimeMillis()) / 1000;
        } catch (Exception e) {
            log.debug("Failed to get token remaining time", e);
            return 0;
        }
    }

    /**
     * 撤销令牌
     */
    public void revokeToken(String token) {
        try {
            DecodedJWT jwt = JWT.decode(token);
            LocalDateTime expiresAt = LocalDateTime.ofInstant(
                jwt.getExpiresAt().toInstant(), ZoneOffset.UTC);
            jwtBlacklistService.addToBlacklist(jwt.getId(), expiresAt);
            
            if (authConfig.getJwt().isEnableAuditLog()) {
                log.info("Token {} revoked for user {}", jwt.getId(), jwt.getSubject());
            }
        } catch (Exception e) {
            log.error("Failed to revoke token", e);
        }
    }
    
    /**
     * 撤销用户的所有令牌
     */
    public void revokeAllUserTokens(String userId) {
        jwtBlacklistService.revokeAllUserTokens(userId);
        
        if (authConfig.getJwt().isEnableAuditLog()) {
            log.info("All tokens revoked for user {}", userId);
        }
    }

    /**
     * 创建自定义令牌
     * 用于RPC调用等特殊场景
     */
    public String createToken(String subject, String type, int expirationSeconds) {
        AuthConfig.Jwt jwtConfig = authConfig.getJwt();
        Algorithm algorithm = Algorithm.HMAC256(jwtConfig.getSecret());
        
        return JWT.create()
                .withIssuer(jwtConfig.getIssuer())
                .withSubject(subject)
                .withJWTId(UUID.randomUUID().toString())
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + (expirationSeconds * 1000L)))
                .withClaim("type", type)
                .sign(algorithm);
    }
    
    /**
     * 刷新令牌对
     */
    public TokenPair refreshTokenPair(String refreshToken) {
        String userId = validateRefreshToken(refreshToken);
        if (userId == null) {
            return null;
        }
        
        // 撤销旧的刷新令牌
        revokeToken(refreshToken);
        
        // 生成新的令牌对
        String sessionId = getSessionIdFromToken(refreshToken);
        String newAccessToken = generateAccessToken(userId, sessionId);
        String newRefreshToken = generateRefreshToken(userId, sessionId);
        
        return new TokenPair(newAccessToken, newRefreshToken);
    }
    
    /**
     * 令牌对数据类
     */
    public static class TokenPair {
        public final String accessToken;
        public final String refreshToken;
        
        public TokenPair(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }
}