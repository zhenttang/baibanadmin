package com.yunke.backend.document.service.impl;

import com.yunke.backend.infrastructure.config.AffineConfig;
import com.yunke.backend.document.dto.DocRecord;
import com.yunke.backend.document.service.DocReader;
import com.yunke.backend.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Optional;

/**
 * RPC文档读取器
 * 从远程文档服务读取文档，失败时回退到数据库读取
 * 参考Node.js版本的RpcDocReader实现
 */
@Service("rpcDocReader")
@ConditionalOnProperty(name = "affine.doc-service.enabled", havingValue = "true")
@Slf4j
public class RpcDocReader implements DocReader {
    
    private final AffineConfig config;
    private final DatabaseDocReader databaseDocReader;
    private final WebClient.Builder webClientBuilder;
    private final JwtUtil jwtUtil;
    
    public RpcDocReader(AffineConfig config, 
                       @Qualifier("databaseDocReader") DatabaseDocReader databaseDocReader,
                       WebClient.Builder webClientBuilder,
                       JwtUtil jwtUtil) {
        this.config = config;
        this.databaseDocReader = databaseDocReader;
        this.webClientBuilder = webClientBuilder;
        this.jwtUtil = jwtUtil;
    }
    
    @Override
    public Mono<Optional<DocRecord>> getDoc(String workspaceId, String docId) {
        if (!isRpcEnabled()) {
            log.debug("RPC模式未启用，直接使用数据库读取");
            return databaseDocReader.getDoc(workspaceId, docId);
        }
        
        return fetchDocFromRpc(workspaceId, docId)
                .onErrorResume(error -> {
                    log.warn("RPC获取文档失败，回退到数据库: workspaceId={}, docId={}, error={}", 
                            workspaceId, docId, error.getMessage());
                    return databaseDocReader.getDoc(workspaceId, docId);
                });
    }
    
    @Override
    public Mono<Optional<byte[]>> getDocSnapshot(String workspaceId, String docId) {
        return getDoc(workspaceId, docId)
                .map(docRecord -> docRecord.map(DocRecord::getBlob));
    }
    
    @Override
    public Mono<byte[]> getDocUpdates(String workspaceId, String docId, LocalDateTime since) {
        if (!isRpcEnabled()) {
            return databaseDocReader.getDocUpdates(workspaceId, docId, since);
        }
        
        return fetchDocUpdatesFromRpc(workspaceId, docId, since)
                .onErrorResume(error -> {
                    log.warn("RPC获取文档更新失败，回退到数据库: workspaceId={}, docId={}, error={}", 
                            workspaceId, docId, error.getMessage());
                    return databaseDocReader.getDocUpdates(workspaceId, docId, since);
                });
    }
    
    @Override
    public Mono<byte[]> getDocDiff(String workspaceId, String docId, byte[] stateVector) {
        if (!isRpcEnabled()) {
            return databaseDocReader.getDocDiff(workspaceId, docId, stateVector);
        }
        
        return fetchDocDiffFromRpc(workspaceId, docId, stateVector)
                .onErrorResume(error -> {
                    log.warn("RPC获取文档差异失败，回退到数据库: workspaceId={}, docId={}, error={}", 
                            workspaceId, docId, error.getMessage());
                    return databaseDocReader.getDocDiff(workspaceId, docId, stateVector);
                });
    }
    
    @Override
    public Mono<Boolean> docExists(String workspaceId, String docId) {
        if (!isRpcEnabled()) {
            return databaseDocReader.docExists(workspaceId, docId);
        }
        
        return checkDocExistsFromRpc(workspaceId, docId)
                .onErrorResume(error -> {
                    log.warn("RPC检查文档存在性失败，回退到数据库: workspaceId={}, docId={}, error={}", 
                            workspaceId, docId, error.getMessage());
                    return databaseDocReader.docExists(workspaceId, docId);
                });
    }
    
    @Override
    public Mono<Optional<LocalDateTime>> getDocLastModified(String workspaceId, String docId) {
        if (!isRpcEnabled()) {
            return databaseDocReader.getDocLastModified(workspaceId, docId);
        }
        
        return getDoc(workspaceId, docId)
                .map(docRecord -> docRecord.map(DocRecord::getTimestamp)
                        .map(ts -> LocalDateTime.ofEpochSecond(ts / 1000, (int) (ts % 1000) * 1_000_000, 
                                java.time.ZoneOffset.UTC)));
    }
    
    // ==================== 私有方法 ====================
    
    private boolean isRpcEnabled() {
        return config.getDocService() != null && 
               config.getDocService().getEndpoint() != null && 
               !config.getDocService().getEndpoint().isEmpty();
    }
    
    private Mono<Optional<DocRecord>> fetchDocFromRpc(String workspaceId, String docId) {
        String url = String.format("%s/rpc/workspaces/%s/docs/%s", 
                config.getDocService().getEndpoint(), workspaceId, docId);
        
        String accessToken = generateAccessToken(docId);
        
        WebClient webClient = webClientBuilder.build();
        
        return webClient.get()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.ACCEPT, "application/octet-stream")
                .retrieve()
                .onStatus(HttpStatus.NOT_FOUND::equals, response -> 
                    Mono.just(new RuntimeException("Document not found")))
                .toEntity(byte[].class)
                .map(responseEntity -> {
                    if (responseEntity.getBody() == null) {
                        return Optional.<DocRecord>empty();
                    }
                    
                    // 从响应头中提取元数据
                    HttpHeaders headers = responseEntity.getHeaders();
                    long timestamp = parseTimestamp(headers.getFirst("x-doc-timestamp"));
                    String editorId = headers.getFirst("x-doc-editor-id");
                    
                    DocRecord docRecord = DocRecord.builder()
                            .spaceId(workspaceId)
                            .docId(docId)
                            .blob(responseEntity.getBody())
                            .timestamp(timestamp)
                            .editorId(editorId)
                            .build();
                    
                    log.info("成功从RPC获取文档: workspaceId={}, docId={}, size={}, timestamp={}", 
                            workspaceId, docId, responseEntity.getBody().length, timestamp);
                    
                    return Optional.of(docRecord);
                })
                .onErrorReturn(Optional.empty());
    }
    
    private Mono<byte[]> fetchDocUpdatesFromRpc(String workspaceId, String docId, LocalDateTime since) {
        String url = String.format("%s/rpc/workspaces/%s/docs/%s/updates", 
                config.getDocService().getEndpoint(), workspaceId, docId);
        
        String accessToken = generateAccessToken(docId);
        long sinceTimestamp = since.atZone(java.time.ZoneOffset.UTC).toInstant().toEpochMilli();
        
        WebClient webClient = webClientBuilder.build();
        
        return webClient.get()
                .uri(uriBuilder -> uriBuilder.path(url)
                        .queryParam("since", sinceTimestamp)
                        .build())
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.ACCEPT, "application/octet-stream")
                .retrieve()
                .bodyToMono(byte[].class)
                .doOnSuccess(data -> log.info("成功从RPC获取文档更新: workspaceId={}, docId={}, size={}", 
                        workspaceId, docId, data != null ? data.length : 0))
                .onErrorReturn(new byte[0]);
    }
    
    private Mono<byte[]> fetchDocDiffFromRpc(String workspaceId, String docId, byte[] stateVector) {
        String url = String.format("%s/rpc/workspaces/%s/docs/%s/diff", 
                config.getDocService().getEndpoint(), workspaceId, docId);
        
        String accessToken = generateAccessToken(docId);
        String stateVectorB64 = Base64.getEncoder().encodeToString(stateVector);
        
        WebClient webClient = webClientBuilder.build();
        
        return webClient.post()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .header(HttpHeaders.ACCEPT, "application/octet-stream")
                .bodyValue("{\"stateVector\":\"" + stateVectorB64 + "\"}")
                .retrieve()
                .bodyToMono(byte[].class)
                .doOnSuccess(data -> log.info("成功从RPC获取文档差异: workspaceId={}, docId={}, size={}", 
                        workspaceId, docId, data != null ? data.length : 0))
                .onErrorReturn(new byte[0]);
    }
    
    private Mono<Boolean> checkDocExistsFromRpc(String workspaceId, String docId) {
        String url = String.format("%s/rpc/workspaces/%s/docs/%s", 
                config.getDocService().getEndpoint(), workspaceId, docId);
        
        String accessToken = generateAccessToken(docId);
        
        WebClient webClient = webClientBuilder.build();
        
        return webClient.head()
                .uri(url)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                .retrieve()
                .toBodilessEntity()
                .map(response -> response.getStatusCode().is2xxSuccessful())
                .onErrorReturn(false);
    }
    
    private String generateAccessToken(String docId) {
        // 使用JWT生成访问令牌，类似Node.js版本的crypto.sign
        return jwtUtil.createToken(docId, "doc-access", 3600); // 1小时有效期
    }
    
    private long parseTimestamp(String timestampStr) {
        if (timestampStr == null || timestampStr.isEmpty()) {
            return System.currentTimeMillis();
        }
        
        try {
            return Long.parseLong(timestampStr);
        } catch (NumberFormatException e) {
            log.warn("无法解析时间戳: {}", timestampStr);
            return System.currentTimeMillis();
        }
    }
}