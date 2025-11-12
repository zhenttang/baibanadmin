package com.yunke.backend.community.controller;


import com.yunke.backend.community.dto.community.TagInfo;
import com.yunke.backend.community.dto.community.CreateTagRequest;
import com.yunke.backend.community.service.DocumentTagService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

/**
 * 文档标签管理Controller
 */
@RestController
@RequestMapping("/api/community/tags")
@Tag(name = "社区标签管理", description = "文档标签的增删改查接口")
public class TagController {
    
    @Autowired
    private DocumentTagService tagService;
    
    /**
     * 获取所有标签（分页）
     */
    @GetMapping
    @Operation(summary = "获取所有标签", description = "分页获取所有标签列表")
    public ResponseEntity<Page<TagInfo>> getAllTags(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<TagInfo> tags = tagService.getAllTags(page, size);
        return ResponseEntity.ok(tags);
    }
    
    /**
     * 获取热门标签
     */
    @GetMapping("/hot")
    @Operation(summary = "获取热门标签", description = "获取使用次数最多的标签列表")
    public ResponseEntity<List<TagInfo>> getHotTags(
            @RequestParam(defaultValue = "10") Integer limit) {
        List<TagInfo> tags = tagService.getHotTags(limit);
        return ResponseEntity.ok(tags);
    }
    
    /**
     * 搜索标签
     */
    @GetMapping("/search")
    @Operation(summary = "搜索标签", description = "根据关键字搜索标签")
    public ResponseEntity<List<TagInfo>> searchTags(@RequestParam String query) {
        List<TagInfo> tags = tagService.searchTags(query);
        return ResponseEntity.ok(tags);
    }
    
    /**
     * 创建标签
     */
    @PostMapping
    @Operation(summary = "创建标签", description = "创建新的标签")
    public ResponseEntity<TagInfo> createTag(@Valid @RequestBody CreateTagRequest request) {
        TagInfo tag = tagService.createTag(request);
        return ResponseEntity.ok(tag);
    }
    
    /**
     * 为文档添加标签
     */
    @PostMapping("/documents/{documentId}")
    @Operation(summary = "为文档添加标签", description = "为指定文档添加一个或多个标签")
    public ResponseEntity<Void> addDocumentTags(
            @PathVariable String documentId,
            @RequestBody List<String> tagNames,
            @RequestHeader("X-User-Id") String userId) {
        tagService.addDocumentTags(documentId, tagNames, userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 移除文档标签
     */
    @DeleteMapping("/documents/{documentId}")
    @Operation(summary = "移除文档标签", description = "移除指定文档的标签")
    public ResponseEntity<Void> removeDocumentTags(
            @PathVariable String documentId,
            @RequestBody List<Integer> tagIds,
            @RequestHeader("X-User-Id") String userId) {
        tagService.removeDocumentTags(documentId, tagIds, userId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取文档的标签列表
     */
    @GetMapping("/documents/{documentId}")
    @Operation(summary = "获取文档标签", description = "获取指定文档的所有标签")
    public ResponseEntity<List<TagInfo>> getDocumentTags(@PathVariable String documentId) {
        List<TagInfo> tags = tagService.getDocumentTags(documentId);
        return ResponseEntity.ok(tags);
    }
    
    /**
     * 删除标签
     */
    @DeleteMapping("/{tagId}")
    @Operation(summary = "删除标签", description = "删除指定标签及其所有关联")
    public ResponseEntity<Void> deleteTag(@PathVariable Integer tagId) {
        tagService.deleteTag(tagId);
        return ResponseEntity.ok().build();
    }
}