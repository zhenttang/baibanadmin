package com.yunke.backend.community.controller;

import com.yunke.backend.community.dto.community.CategoryInfo;
import com.yunke.backend.community.dto.community.CreateCategoryRequest;
import com.yunke.backend.community.dto.community.UpdateCategoryRequest;
import com.yunke.backend.community.service.DocumentCategoryService;
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
 * 文档分类管理Controller
 */
@RestController
@RequestMapping("/api/community/categories")
@Tag(name = "社区分类管理", description = "文档分类的增删改查接口")
public class CategoryController {
    
    @Autowired
    private DocumentCategoryService categoryService;
    
    /**
     * 获取所有激活的分类
     */
    @GetMapping
    @Operation(summary = "获取所有分类", description = "获取所有激活状态的分类列表")
    public ResponseEntity<List<CategoryInfo>> getAllCategories() {
        List<CategoryInfo> categories = categoryService.getAllActiveCategories();
        return ResponseEntity.ok(categories);
    }
    
    /**
     * 创建分类（管理员接口）
     */
    @PostMapping
    @Operation(summary = "创建分类", description = "创建新的文档分类")
    public ResponseEntity<CategoryInfo> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        CategoryInfo category = categoryService.createCategory(request);
        return ResponseEntity.ok(category);
    }
    
    /**
     * 更新分类（管理员接口）
     */
    @PutMapping("/{categoryId}")
    @Operation(summary = "更新分类", description = "更新指定分类的信息")
    public ResponseEntity<CategoryInfo> updateCategory(
            @PathVariable Integer categoryId,
            @Valid @RequestBody UpdateCategoryRequest request) {
        CategoryInfo category = categoryService.updateCategory(categoryId, request);
        return ResponseEntity.ok(category);
    }
    
    /**
     * 删除分类（管理员接口）
     */
    @DeleteMapping("/{categoryId}")
    @Operation(summary = "删除分类", description = "删除指定的分类")
    public ResponseEntity<Void> deleteCategory(@PathVariable Integer categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.ok().build();
    }
    
    /**
     * 获取分类下的文档
     */
    @GetMapping("/{categoryId}/documents")
    @Operation(summary = "获取分类文档", description = "获取指定分类下的文档列表")
    public ResponseEntity<Page<CategoryInfo>> getCategoryDocuments(
            @PathVariable Integer categoryId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        Page<CategoryInfo> documents = categoryService.getCategoryDocuments(categoryId, page, size);
        return ResponseEntity.ok(documents);
    }
}