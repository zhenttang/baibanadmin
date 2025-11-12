package com.yunke.backend.security.aspect;

import com.yunke.backend.common.exception.PermissionDeniedException;
import com.yunke.backend.security.service.PermissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限检查 AOP 切面
 * 
 * <p>使用 {@link RequireWorkspacePermission} 或 {@link RequireDocPermission} 注解
 * 可以自动进行权限检查，无需在方法内部手动调用权限检查代码。</p>
 * 
 * <p>示例：
 * <pre>
 * {@code
 * @RequireWorkspacePermission(action = "read")
 * public Mono<WorkspaceDto> getWorkspace(String workspaceId, String userId) {
 *     // 方法实现，权限检查已自动完成
 * }
 * }
 * </pre>
 * </p>
 * 
 * @author System
 * @since 2024-12-19
 */
@Aspect
@Component
@Order(1) // 在事务切面之前执行
@RequiredArgsConstructor
@Slf4j
public class PermissionAspect {
    
    private final PermissionService permissionService;
    
    /**
     * 要求工作空间权限注解
     * 
     * <p>在方法上使用此注解，AOP 切面会自动检查用户是否具有指定工作空间的权限。
     * 方法参数中必须包含 workspaceId 和 userId。</p>
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequireWorkspacePermission {
        /**
         * 权限动作（如 "read", "write", "delete"）
         * 可以使用 PermissionActions 常量
         */
        String action();
        
        /**
         * workspaceId 参数名（默认为 "workspaceId"）
         */
        String workspaceIdParam() default "workspaceId";
        
        /**
         * userId 参数名（默认为 "userId"）
         */
        String userIdParam() default "userId";
    }
    
    /**
     * 要求文档权限注解
     * 
     * <p>在方法上使用此注解，AOP 切面会自动检查用户是否具有指定文档的权限。
     * 方法参数中必须包含 workspaceId、docId 和 userId。</p>
     */
    @Target(ElementType.METHOD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface RequireDocPermission {
        /**
         * 权限动作（如 "read", "write", "delete"）
         * 可以使用 PermissionActions 常量
         */
        String action();
        
        /**
         * workspaceId 参数名（默认为 "workspaceId"）
         */
        String workspaceIdParam() default "workspaceId";
        
        /**
         * docId 参数名（默认为 "docId"）
         */
        String docIdParam() default "docId";
        
        /**
         * userId 参数名（默认为 "userId"）
         */
        String userIdParam() default "userId";
    }
    
    /**
     * 拦截带有 @RequireWorkspacePermission 注解的方法
     */
    @Around("@annotation(com.yunke.backend.security.aspect.PermissionAspect.RequireWorkspacePermission)")
    public Object checkWorkspacePermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequireWorkspacePermission annotation = signature.getMethod()
                .getAnnotation(RequireWorkspacePermission.class);
        
        String action = annotation.action();
        String workspaceIdParam = annotation.workspaceIdParam();
        String userIdParam = annotation.userIdParam();
        
        // 获取参数值
        Object[] args = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();
        
        String workspaceId = null;
        String userId = null;
        
        for (int i = 0; i < paramNames.length; i++) {
            if (workspaceIdParam.equals(paramNames[i])) {
                workspaceId = (String) args[i];
            }
            if (userIdParam.equals(paramNames[i])) {
                userId = (String) args[i];
            }
        }
        
        if (workspaceId == null || userId == null) {
            log.warn("Missing workspaceId or userId parameter in method: {}", signature.getMethod().getName());
            return joinPoint.proceed();
        }
        
        // 创建 final 副本供 lambda 使用
        final String finalWorkspaceId = workspaceId;
        final String finalUserId = userId;
        final String finalAction = action;
        
        // 检查权限
        return permissionService.checkWorkspacePermission(finalWorkspaceId, finalUserId, finalAction)
                .flatMap(hasPermission -> {
                    if (!hasPermission) {
                        log.warn("Permission denied: user {} does not have {} permission on workspace {}", 
                                finalUserId, finalAction, finalWorkspaceId);
                        return Mono.error(new PermissionDeniedException(finalUserId, "workspace", finalAction));
                    }
                    try {
                        Object result = joinPoint.proceed();
                        if (result instanceof Mono) {
                            return (Mono<?>) result;
                        }
                        return Mono.just(result);
                    } catch (Throwable e) {
                        return Mono.error(e);
                    }
                });
    }
    
    /**
     * 拦截带有 @RequireDocPermission 注解的方法
     */
    @Around("@annotation(com.yunke.backend.security.aspect.PermissionAspect.RequireDocPermission)")
    public Object checkDocPermission(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        RequireDocPermission annotation = signature.getMethod()
                .getAnnotation(RequireDocPermission.class);
        
        String action = annotation.action();
        String workspaceIdParam = annotation.workspaceIdParam();
        String docIdParam = annotation.docIdParam();
        String userIdParam = annotation.userIdParam();
        
        // 获取参数值
        Object[] args = joinPoint.getArgs();
        String[] paramNames = signature.getParameterNames();
        
        String workspaceId = null;
        String docId = null;
        String userId = null;
        
        for (int i = 0; i < paramNames.length; i++) {
            if (workspaceIdParam.equals(paramNames[i])) {
                workspaceId = (String) args[i];
            }
            if (docIdParam.equals(paramNames[i])) {
                docId = (String) args[i];
            }
            if (userIdParam.equals(paramNames[i])) {
                userId = (String) args[i];
            }
        }
        
        if (workspaceId == null || docId == null || userId == null) {
            log.warn("Missing workspaceId, docId or userId parameter in method: {}", signature.getMethod().getName());
            return joinPoint.proceed();
        }
        
        // 创建 final 副本供 lambda 使用
        final String finalWorkspaceId = workspaceId;
        final String finalDocId = docId;
        final String finalUserId = userId;
        final String finalAction = action;
        
        // 检查权限
        return permissionService.checkDocPermission(finalWorkspaceId, finalDocId, finalUserId, finalAction)
                .flatMap(hasPermission -> {
                    if (!hasPermission) {
                        log.warn("Permission denied: user {} does not have {} permission on document {}/{}", 
                                finalUserId, finalAction, finalWorkspaceId, finalDocId);
                        return Mono.error(new PermissionDeniedException(finalUserId, "document", finalAction));
                    }
                    try {
                        Object result = joinPoint.proceed();
                        if (result instanceof Mono) {
                            return (Mono<?>) result;
                        }
                        return Mono.just(result);
                    } catch (Throwable e) {
                        return Mono.error(e);
                    }
                });
    }
}

