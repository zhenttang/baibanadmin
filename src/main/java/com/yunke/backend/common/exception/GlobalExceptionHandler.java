package com.yunke.backend.common.exception;

import com.yunke.backend.common.ApiResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.bind.support.WebExchangeBindException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 全局异常处理器
 * 统一处理所有业务异常和系统异常
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理权限拒绝异常
     */
    @ExceptionHandler(PermissionDeniedException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handlePermissionDenied(PermissionDeniedException ex) {
        log.warn("Permission denied: {}", ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.error(ex.getMessage())));
    }

    /**
     * 处理资源未找到异常
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleResourceNotFound(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.error(ex.getMessage())));
    }

    /**
     * 处理验证异常
     */
    @ExceptionHandler(ValidationException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleValidationException(ValidationException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage())));
    }

    /**
     * 处理冲突异常
     */
    @ExceptionHandler(ConflictException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleConflictException(ConflictException ex) {
        log.warn("Conflict: {}", ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.error(ex.getMessage())));
    }

    /**
     * 处理业务异常
     */
    @ExceptionHandler(BusinessException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleBusinessException(BusinessException ex) {
        log.warn("Business error: {}", ex.getMessage(), ex);
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage())));
    }

    /**
     * 处理参数绑定异常（Spring WebFlux）
     */
    @ExceptionHandler(WebExchangeBindException.class)
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> handleWebExchangeBindException(
            WebExchangeBindException ex) {
        log.warn("Binding error: {}", ex.getMessage());
        
        Map<String, Object> errors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        error -> error.getField(),
                        error -> error.getDefaultMessage() != null ? error.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));
        
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("errors", errors);
        
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error("Validation failed", errorData)));
    }

    /**
     * 处理输入异常（Spring WebFlux）
     */
    @ExceptionHandler(ServerWebInputException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleServerWebInputException(ServerWebInputException ex) {
        log.warn("Input error: {}", ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getReason() != null ? ex.getReason() : "Invalid input")));
    }

    /**
     * 处理非法参数异常
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage())));
    }

    /**
     * 处理非法状态异常
     */
    @ExceptionHandler(IllegalStateException.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleIllegalStateException(IllegalStateException ex) {
        log.warn("Illegal state: {}", ex.getMessage());
        return Mono.just(ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ex.getMessage())));
    }

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ApiResponse<Void>>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.error("An unexpected error occurred. Please try again later.")));
    }
}

