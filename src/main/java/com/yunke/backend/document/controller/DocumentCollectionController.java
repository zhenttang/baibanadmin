package com.yunke.backend.document.controller;


import com.yunke.backend.community.dto.request.CollectRequest;
import com.yunke.backend.community.dto.response.CollectedDocumentInfo;
import com.yunke.backend.document.service.DocumentCollectionService;
import com.yunke.backend.common.ApiResponse;
import com.yunke.backend.common.PageResponse;
import org.springframework.data.domain.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

/**
 * 文档收藏控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/community/collections")
@Tag(name = "文档收藏API", description = "文档收藏相关接口")
public class DocumentCollectionController {
    
    @Autowired
    private DocumentCollectionService documentCollectionService;
    
    /**
     * 收藏文档
     */
    @PostMapping("/{documentId}")
    @Operation(summary = "收藏文档", description = "用户收藏指定文档")
    public ApiResponse<Void> collectDocument(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            @Parameter(description = "收藏请求") @RequestBody @Valid CollectRequest request,
            @Parameter(description = "用户ID") @RequestHeader("X-User-Id") String userId) {
        
        log.info("API调用：收藏文档，文档ID：{}，用户ID：{}，收藏夹：{}", documentId, userId, request.getCollectionName());
        
        try {
            documentCollectionService.collectDocument(documentId, userId, request);
            return ApiResponse.success("收藏成功");
        } catch (Exception e) {
            log.error("收藏文档失败：", e);
            return ApiResponse.error("收藏失败：" + e.getMessage());
        }
    }
    
    /**
     * 取消收藏文档
     */
    @DeleteMapping("/{documentId}")
    @Operation(summary = "取消收藏文档", description = "用户取消收藏指定文档")
    public ApiResponse<Void> uncollectDocument(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            @Parameter(description = "用户ID") @RequestHeader("X-User-Id") String userId) {
        
        log.info("API调用：取消收藏文档，文档ID：{}，用户ID：{}", documentId, userId);
        
        try {
            documentCollectionService.uncollectDocument(documentId, userId);
            return ApiResponse.success("取消收藏成功");
        } catch (Exception e) {
            log.error("取消收藏文档失败：", e);
            return ApiResponse.error("取消收藏失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取用户收藏列表
     */
    @GetMapping("/user")
    @Operation(summary = "获取用户收藏列表", description = "分页获取用户的收藏文档列表")
    public ApiResponse<PageResponse<CollectedDocumentInfo>> getUserCollections(
            @Parameter(description = "用户ID") @RequestHeader("X-User-Id") String userId,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        
        log.info("API调用：获取用户收藏列表，用户ID：{}，页码：{}，大小：{}", userId, page, size);
        
        try {
            Page<CollectedDocumentInfo> result = documentCollectionService.getUserCollections(userId, page, size);
            
            PageResponse<CollectedDocumentInfo> pageResponse = new PageResponse<>();
            pageResponse.setItems(result.getContent());
            pageResponse.setPage(result.getNumber());
            pageResponse.setSize(result.getSize());
            pageResponse.setTotal(result.getTotalElements());
            pageResponse.setTotalPages(result.getTotalPages());
            
            return ApiResponse.success(pageResponse);
        } catch (Exception e) {
            log.error("获取用户收藏列表失败：", e);
            return ApiResponse.error("获取收藏列表失败：" + e.getMessage());
        }
    }
    
    /**
     * 检查文档收藏状态
     */
    @GetMapping("/{documentId}/status")
    @Operation(summary = "检查文档收藏状态", description = "检查用户是否已收藏指定文档")
    public ApiResponse<Boolean> checkCollectionStatus(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            @Parameter(description = "用户ID") @RequestHeader("X-User-Id") String userId) {
        
        log.info("API调用：检查文档收藏状态，文档ID：{}，用户ID：{}", documentId, userId);
        
        try {
            boolean isCollected = documentCollectionService.isCollected(documentId, userId);
            return ApiResponse.success(isCollected);
        } catch (Exception e) {
            log.error("检查文档收藏状态失败：", e);
            return ApiResponse.error("检查收藏状态失败：" + e.getMessage());
        }
    }
}