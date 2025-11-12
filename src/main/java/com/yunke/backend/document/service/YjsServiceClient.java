package com.yunke.backend.document.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * YJS微服务客户端
 *
 * 调用Node.js YJS微服务进行CRDT操作
 * 支持合并、差异计算、状态向量提取等功能
 */
@Service
public class YjsServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(YjsServiceClient.class);

    @Value("${yjs.service.url:http://localhost:3001}")
    private String yjsServiceUrl;

    @Value("${yjs.service.urls:}")
    private String yjsServiceUrls;

    @Value("${yjs.service.timeout:5000}")
    private int timeout;

    @Value("${yjs.service.retry:3}")
    private int maxRetry;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    private final AtomicInteger roundRobinCounter = new AtomicInteger();
    private final CopyOnWriteArrayList<String> serviceUrlPool = new CopyOnWriteArrayList<>();

    public YjsServiceClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @jakarta.annotation.PostConstruct
    void initServiceUrls() {
        if (yjsServiceUrls != null && !yjsServiceUrls.isBlank()) {
            java.util.Arrays.stream(yjsServiceUrls.split(","))
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .map(this::normalizeBaseUrl)
                .forEach(serviceUrlPool::add);
        }

        if (serviceUrlPool.isEmpty()) {
            serviceUrlPool.add(normalizeBaseUrl(yjsServiceUrl));
        }
    }

    /**
     * 合并多个YJS更新
     *
     * @param updates 更新列表（二进制）
     * @return 合并后的更新（二进制）
     */
    public byte[] mergeUpdates(List<byte[]> updates) {
        long totalBytes = updates.stream()
            .filter(update -> update != null)
            .mapToLong(update -> update.length)
            .sum();
        logger.info(
            "🔄 [YjsServiceClient] 调用微服务合并{}个更新，总大小={}B",
            updates.size(),
            totalBytes
        );
        if (totalBytes > 16L * 1024 * 1024) {
            logger.warn(
                "⚠️ [YjsServiceClient] 合并批次较大 ({}B)，建议优化前端增量或调整批次阈值",
                totalBytes
            );
        }

        try {
            // 转换为Base64
            List<String> base64Updates = updates.stream()
                .map(update -> {
                    String base64 = Base64.getEncoder().encodeToString(update);
                    // 显示前16字节的十六进制
                    StringBuilder hex = new StringBuilder();
                    for (int i = 0; i < Math.min(16, update.length); i++) {
                        hex.append(String.format("%02x ", update[i] & 0xFF));
                    }
                    logger.info("  📦 更新: {}字节, 前16字节: {}", update.length, hex.toString().trim());
                    return base64;
                })
                .collect(Collectors.toList());

            // 构建请求
            Map<String, Object> request = new HashMap<>();
            request.put("updates", base64Updates);

            // 调用微服务
            Map<String, Object> response = callServiceWithRetry("/api/yjs/merge", request);

            // 解析响应
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                String mergedBase64 = (String) response.get("merged");
                Integer size = (Integer) response.get("size");

                byte[] merged = Base64.getDecoder().decode(mergedBase64);
                logger.info("✅ [YjsServiceClient] 合并成功: {}个 → {}字节", updates.size(), size);

                return merged;
            } else {
                String error = response != null ? (String) response.get("error") : "unknown";
                throw new RuntimeException("YJS微服务合并失败: " + error);
            }

        } catch (Exception e) {
            logger.error("❌ [YjsServiceClient] 调用微服务失败", e);
            throw new RuntimeException("YJS微服务调用失败: " + e.getMessage(), e);
        }
    }

    /**
     * 计算差异更新
     *
     * @param update 完整更新
     * @param stateVector 客户端状态向量（可选）
     * @return 差异更新
     */
    public byte[] diffUpdate(byte[] update, byte[] stateVector) {
        logger.debug("🔍 [YjsServiceClient] 计算差异更新");

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("update", Base64.getEncoder().encodeToString(update));

            if (stateVector != null && stateVector.length > 0) {
                request.put("stateVector", Base64.getEncoder().encodeToString(stateVector));
            }

            Map<String, Object> response = callServiceWithRetry("/api/yjs/diff", request);

            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                String diffBase64 = (String) response.get("diff");
                return Base64.getDecoder().decode(diffBase64);
            } else {
                logger.warn("差异计算失败，返回完整更新");
                return update;
            }

        } catch (Exception e) {
            logger.error("❌ [YjsServiceClient] 差异计算失败", e);
            return update; // 失败时返回完整更新
        }
    }

    /**
     * 提取状态向量
     *
     * @param update 更新数据
     * @return 状态向量
     */
    public byte[] encodeStateVector(byte[] update) {
        logger.debug("📊 [YjsServiceClient] 提取状态向量");

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("update", Base64.getEncoder().encodeToString(update));

            Map<String, Object> response = callServiceWithRetry("/api/yjs/state-vector", request);

            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                String stateVectorBase64 = (String) response.get("stateVector");
                return Base64.getDecoder().decode(stateVectorBase64);
            } else {
                throw new RuntimeException("状态向量提取失败");
            }

        } catch (Exception e) {
            logger.error("❌ [YjsServiceClient] 状态向量提取失败", e);
            return new byte[0];
        }
    }

    /**
     * 应用更新到文档
     *
     * @param currentDoc 当前文档
     * @param update 新更新
     * @return 应用后的文档
     */
    public byte[] applyUpdate(byte[] currentDoc, byte[] update) {
        logger.debug("🔄 [YjsServiceClient] 应用更新到文档");

        try {
            Map<String, Object> request = new HashMap<>();

            if (currentDoc != null && currentDoc.length > 0) {
                request.put("currentDoc", Base64.getEncoder().encodeToString(currentDoc));
            }
            request.put("update", Base64.getEncoder().encodeToString(update));

            Map<String, Object> response = callServiceWithRetry("/api/yjs/apply", request);

            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                String resultBase64 = (String) response.get("result");
                return Base64.getDecoder().decode(resultBase64);
            } else {
                throw new RuntimeException("应用更新失败");
            }

        } catch (Exception e) {
            logger.error("❌ [YjsServiceClient] 应用更新失败", e);
            throw new RuntimeException("应用更新失败: " + e.getMessage(), e);
        }
    }

    /**
     * 批量合并多个文档
     *
     * @param batches 批量合并请求列表
     * @return 批量合并结果列表
     */
    public List<BatchMergeResult> batchMerge(List<BatchMergeRequest> batches) {
        logger.info("📦 [YjsServiceClient] 批量合并{}个文档", batches.size());

        try {
            // 构建请求
            List<Map<String, Object>> requestBatches = batches.stream()
                .map(batch -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("docId", batch.getDocId());
                    item.put("updates", batch.getUpdates().stream()
                        .map(update -> Base64.getEncoder().encodeToString(update))
                        .collect(Collectors.toList()));
                    return item;
                })
                .collect(Collectors.toList());

            Map<String, Object> request = new HashMap<>();
            request.put("batches", requestBatches);

            // 调用微服务
            Map<String, Object> response = callServiceWithRetry("/api/yjs/batch-merge", request);

            // 解析响应
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");

                return results.stream()
                    .map(result -> {
                        String docId = (String) result.get("docId");
                        Boolean success = (Boolean) result.get("success");

                        if (Boolean.TRUE.equals(success)) {
                            String mergedBase64 = (String) result.get("merged");
                            byte[] merged = Base64.getDecoder().decode(mergedBase64);
                            return new BatchMergeResult(docId, true, merged, null);
                        } else {
                            String error = (String) result.get("error");
                            return new BatchMergeResult(docId, false, null, error);
                        }
                    })
                    .collect(Collectors.toList());
            } else {
                throw new RuntimeException("批量合并失败");
            }

        } catch (Exception e) {
            logger.error("❌ [YjsServiceClient] 批量合并失败", e);
            throw new RuntimeException("批量合并失败: " + e.getMessage(), e);
        }
    }

    /**
     * 创建空的YJS文档
     * 
     * 通过 yjs-service 使用官方 yjs 库创建标准的空 Y.js 文档
     * 
     * ⚠️ 重要：不要在 Java 中手动构造 Y.js 二进制格式！
     * 所有 Y.js CRDT 操作都必须通过官方 yjs 库（Node.js）处理，
     * 以确保二进制格式 100% 兼容。
     *
     * @param docId 文档ID（可选，用于日志）
     * @return 有效的空 Y.js 文档二进制数据
     */
    public byte[] createEmptyDoc(String docId) {
        logger.info("📄 [YjsServiceClient] 请求创建空YJS文档: docId={}", docId != null ? docId : "unknown");

        try {
            // 构建请求
            Map<String, Object> request = new HashMap<>();
            if (docId != null) {
                request.put("docId", docId);
            }

            // 调用微服务
            Map<String, Object> response = callServiceWithRetry("/api/yjs/create-empty", request);

            // 解析响应
            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                String emptyBase64 = (String) response.get("empty");
                Integer size = (Integer) response.get("size");
                String hexPreview = (String) response.get("hexPreview");

                byte[] emptyDoc = Base64.getDecoder().decode(emptyBase64);

                logger.info("✅ [YjsServiceClient] 空文档创建成功: {}字节", size);
                logger.debug("🔍 [YjsServiceClient] 二进制预览: {}", hexPreview);

                return emptyDoc;
            } else {
                String error = response != null ? (String) response.get("error") : "unknown";
                throw new RuntimeException("YJS微服务创建空文档失败: " + error);
            }

        } catch (Exception e) {
            logger.error("❌ [YjsServiceClient] 创建空文档失败", e);
            throw new RuntimeException("YJS微服务创建空文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 检查YJS服务健康状态
     *
     * @return 服务是否健康
     */
    public boolean checkHealth() {
        try {
            for (String baseUrl : serviceUrlPool) {
                String url = baseUrl + "/health";
                try {
                    Map<String, Object> response = restTemplate.getForObject(url, Map.class);
                    if (response != null && "ok".equals(response.get("status"))) {
                        return true;
                    }
                } catch (Exception singleError) {
                    logger.debug("⚠️ [YjsServiceClient] 健康检测节点 {} 失败: {}", baseUrl, singleError.getMessage());
                }
            }
            return false;
        } catch (Exception e) {
            logger.warn("⚠️ [YjsServiceClient] 健康检查失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 调用微服务（带重试）
     */
    private Map<String, Object> callServiceWithRetry(String path, Map<String, Object> request) {
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < maxRetry) {
            try {
                String baseUrl = selectServiceUrl();
                String url = baseUrl + path;

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);

                ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, entity, Map.class);

                if (responseEntity.getStatusCode().is2xxSuccessful()) {
                    return responseEntity.getBody();
                } else {
                    throw new RestClientException("HTTP " + responseEntity.getStatusCode());
                }

            } catch (Exception e) {
                lastException = e;
                retryCount++;

                if (retryCount < maxRetry) {
                    logger.warn("⚠️ [YjsServiceClient] 调用失败，重试 {}/{}: {}",
                               retryCount, maxRetry, e.getMessage());

                    try {
                        Thread.sleep(100 * retryCount); // 指数退避
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        logger.error("❌ [YjsServiceClient] 调用失败，已重试{}次", maxRetry);
        throw new RuntimeException("YJS微服务调用失败", lastException);
    }

    private String selectServiceUrl() {
        int idx = Math.abs(roundRobinCounter.getAndIncrement());
        if (serviceUrlPool.isEmpty()) {
            return normalizeBaseUrl(yjsServiceUrl);
        }
        return serviceUrlPool.get(idx % serviceUrlPool.size());
    }

    private String normalizeBaseUrl(String url) {
        if (url == null || url.isBlank()) {
            return "http://localhost:3001";
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    // ==================== 内部类 ====================

    /**
     * 批量合并请求
     */
    public static class BatchMergeRequest {
        private final String docId;
        private final List<byte[]> updates;

        public BatchMergeRequest(String docId, List<byte[]> updates) {
            this.docId = docId;
            this.updates = updates;
        }

        public String getDocId() { return docId; }
        public List<byte[]> getUpdates() { return updates; }
    }

    /**
     * 批量合并结果
     */
    public static class BatchMergeResult {
        private final String docId;
        private final boolean success;
        private final byte[] merged;
        private final String error;

        public BatchMergeResult(String docId, boolean success, byte[] merged, String error) {
            this.docId = docId;
            this.success = success;
            this.merged = merged;
            this.error = error;
        }

        public String getDocId() { return docId; }
        public boolean isSuccess() { return success; }
        public byte[] getMerged() { return merged; }
        public String getError() { return error; }
    }
}
