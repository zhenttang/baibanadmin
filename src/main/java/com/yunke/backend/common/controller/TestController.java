package com.yunke.backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

/**
 * 测试控制器，用于调试和开发
 */
@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
@Slf4j
public class TestController {

    /**
     * 记录所有请求信息的测试端点
     */
    @GetMapping("/log-request")
    public ResponseEntity<Map<String, Object>> logRequest(HttpServletRequest request) {
        log.info("接收到测试请求: {}", request.getRequestURI());
        
        // 记录请求头
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        log.info("请求头: {}", headers);
        
        // 记录请求参数
        Map<String, String[]> parameters = request.getParameterMap();
        log.info("请求参数: {}", parameters);
        
        // 记录其他信息
        log.info("请求方法: {}", request.getMethod());
        log.info("请求路径: {}", request.getRequestURI());
        log.info("请求查询参数: {}", request.getQueryString());
        log.info("客户端IP: {}", request.getRemoteAddr());
        
        // 返回信息
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "请求信息已记录");
        response.put("headers", headers);
        response.put("parameters", parameters);
        response.put("uri", request.getRequestURI());
        response.put("method", request.getMethod());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * 测试Accept头处理的端点
     */
    @GetMapping("/accept-test")
    public ResponseEntity<Map<String, Object>> testAcceptHeader(
            @RequestHeader(value = "Accept", required = false) String acceptHeader,
            HttpServletRequest request) {
        
        log.info("接收到Accept测试请求: {}", request.getRequestURI());
        log.info("Accept头: {}", acceptHeader);
        
        // 记录所有请求头
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> headerNames = request.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.put(headerName, request.getHeader(headerName));
        }
        log.info("所有请求头: {}", headers);
        
        // 构建响应
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "Accept测试成功");
        response.put("receivedAcceptHeader", acceptHeader);
        response.put("allHeaders", headers);
        
        // 根据Accept头选择响应格式
        if (acceptHeader != null) {
            if (acceptHeader.contains("application/json") || acceptHeader.contains("*/*")) {
                log.info("返回JSON响应");
                return ResponseEntity.ok()
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(response);
            } else {
                log.warn("不支持的Accept头: {}", acceptHeader);
                Map<String, Object> errorResponse = new HashMap<>();
                errorResponse.put("error", "不支持的Accept类型");
                errorResponse.put("supportedTypes", "application/json");
                errorResponse.put("receivedAcceptHeader", acceptHeader);
                
                return ResponseEntity.status(406)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(errorResponse);
            }
        }
        
        // 默认返回JSON
        log.info("未提供Accept头，默认返回JSON");
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
} 