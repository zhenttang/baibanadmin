package com.yunke.backend.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Map;

/**
 * Worker控制器 - 处理代理和工具类请求
 */
@RestController
@RequestMapping("/api/worker")
@RequiredArgsConstructor
@Slf4j
public class WorkerController {

    private final RestTemplate restTemplate;

    /**
     * 图片代理接口 - 代理图片请求以避免CORS问题
     */
    @GetMapping("/image-proxy")
    public ResponseEntity<byte[]> imageProxy(@RequestParam String url) {
        log.info("图片代理请求: {}", url);
        
        try {
            // 验证URL格式
            URL imageUrl = new URL(url);
            String protocol = imageUrl.getProtocol();
            
            // 只允许HTTP和HTTPS协议
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                log.warn("不支持的URL协议: {}", protocol);
                return ResponseEntity.badRequest().build();
            }
            
            // 建立连接并获取图片数据
            URLConnection connection = imageUrl.openConnection();
            connection.setConnectTimeout(10000); // 10秒连接超时
            connection.setReadTimeout(30000);    // 30秒读取超时
            connection.setRequestProperty("User-Agent", 
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36");
            
            // 获取内容类型
            String contentType = connection.getContentType();
            if (contentType == null) {
                contentType = "image/png"; // 默认类型
            }
            
            // 验证是否为图片类型
            if (!contentType.startsWith("image/")) {
                log.warn("请求的URL不是图片类型: {}", contentType);
                return ResponseEntity.badRequest()
                    .body("请求的URL不是图片类型".getBytes());
            }
            
            // 读取图片数据
            byte[] imageData = connection.getInputStream().readAllBytes();
            
            // 设置响应头
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(imageData.length);
            
            // 添加缓存控制头
            headers.setCacheControl("public, max-age=3600"); // 缓存1小时
            
            // 添加CORS头
            headers.add("Access-Control-Allow-Origin", "*");
            headers.add("Access-Control-Allow-Methods", "GET");
            headers.add("Access-Control-Allow-Headers", "*");
            
            log.info("成功代理图片: {} bytes, 类型: {}", imageData.length, contentType);
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(imageData);
                
        } catch (IOException e) {
            log.error("图片代理失败: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body("无法获取图片数据".getBytes());
        } catch (Exception e) {
            log.error("图片代理出现未知错误: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("服务器内部错误".getBytes());
        }
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
            "status", "ok",
            "service", "worker",
            "timestamp", System.currentTimeMillis()
        ));
    }
}