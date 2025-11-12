package com.yunke.backend.document.controller;


import com.yunke.backend.community.dto.response.LikeStatusResponse;
import com.yunke.backend.community.dto.response.UserLikeInfo;
import com.yunke.backend.document.service.DocumentLikeService;
import com.yunke.backend.common.ApiResponse;
import com.yunke.backend.common.PageResponse;
import org.springframework.data.domain.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * 文档点赞控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/community/likes")
@Tag(name = "文档点赞API", description = "文档点赞相关接口")
public class DocumentLikeController {
    
    @Autowired
    private DocumentLikeService documentLikeService;
    
    /**
     * 点赞文档
     */
    @PostMapping("/{documentId}")
    @Operation(summary = "点赞文档", description = "用户点赞指定文档")
    public ApiResponse<Void> likeDocument(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            @Parameter(description = "用户ID") @RequestHeader("X-User-Id") String userId) {
        
        log.info("API调用：点赞文档，文档ID：{}，用户ID：{}", documentId, userId);
        
        try {
            documentLikeService.likeDocument(documentId, userId);
            return ApiResponse.success("点赞成功");
        } catch (Exception e) {
            log.error("点赞文档失败：", e);
            return ApiResponse.error("点赞失败：" + e.getMessage());
        }
    }
    
    /**
     * 取消点赞文档
     */
    @DeleteMapping("/{documentId}")
    @Operation(summary = "取消点赞文档", description = "用户取消点赞指定文档")
    public ApiResponse<Void> unlikeDocument(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            @Parameter(description = "用户ID") @RequestHeader("X-User-Id") String userId) {
        
        log.info("API调用：取消点赞文档，文档ID：{}，用户ID：{}", documentId, userId);
        
        try {
            documentLikeService.unlikeDocument(documentId, userId);
            return ApiResponse.success("取消点赞成功");
        } catch (Exception e) {
            log.error("取消点赞文档失败：", e);
            return ApiResponse.error("取消点赞失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取文档点赞状态
     */
    @GetMapping("/{documentId}/status")
    @Operation(summary = "获取文档点赞状态", description = "获取用户对指定文档的点赞状态")
    public ApiResponse<LikeStatusResponse> getLikeStatus(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            @Parameter(description = "用户ID") @RequestHeader("X-User-Id") String userId) {
        
        log.info("API调用：获取文档点赞状态，文档ID：{}，用户ID：{}", documentId, userId);
        
        try {
            LikeStatusResponse response = documentLikeService.getLikeStatus(documentId, userId);
            return ApiResponse.success(response);
        } catch (Exception e) {
            log.error("获取文档点赞状态失败：", e);
            return ApiResponse.error("获取点赞状态失败：" + e.getMessage());
        }
    }
    
    /**
     * 获取文档点赞用户列表
     */
    @GetMapping("/{documentId}")
    @Operation(summary = "获取文档点赞用户列表", description = "分页获取点赞指定文档的用户列表")
    public ApiResponse<PageResponse<UserLikeInfo>> getDocumentLikes(
            @Parameter(description = "文档ID") @PathVariable String documentId,
            @Parameter(description = "页码，从0开始") @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") int size) {
        
        log.info("API调用：获取文档点赞用户列表，文档ID：{}，页码：{}，大小：{}", documentId, page, size);
        
        try {
            Page<UserLikeInfo> result = documentLikeService.getDocumentLikes(documentId, page, size);
            
            PageResponse<UserLikeInfo> pageResponse = new PageResponse<>();
            pageResponse.setItems(result.getContent());
            pageResponse.setPage(result.getNumber());
            pageResponse.setSize(result.getSize());
            pageResponse.setTotal(result.getTotalElements());
            pageResponse.setTotalPages(result.getTotalPages());
            
            return ApiResponse.success(pageResponse);
        } catch (Exception e) {
            log.error("获取文档点赞用户列表失败：", e);
            return ApiResponse.error("获取点赞用户列表失败：" + e.getMessage());
        }
    }
}