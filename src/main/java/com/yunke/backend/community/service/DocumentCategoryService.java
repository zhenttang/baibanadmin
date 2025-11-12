package com.yunke.backend.community.service;

import com.yunke.backend.community.dto.community.CategoryInfo;
import com.yunke.backend.community.dto.community.CreateCategoryRequest;
import com.yunke.backend.community.dto.community.UpdateCategoryRequest;
import com.yunke.backend.document.domain.entity.DocumentCategory;
import com.yunke.backend.document.repository.DocumentCategoryRepository;
import com.yunke.backend.community.repository.CommunityDocumentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 文档分类服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentCategoryService {
    
    private final DocumentCategoryRepository documentCategoryRepository;
    private final CommunityDocumentRepository communityDocumentRepository;
    
    /**
     * 获取所有激活的分类
     */
    public List<CategoryInfo> getAllActiveCategories() {
        List<DocumentCategory> categories = documentCategoryRepository.findByIsActiveTrue();
        return categories.stream().map(this::convertToInfo).collect(Collectors.toList());
    }
    
    /**
     * 创建分类
     */
    @Transactional(rollbackFor = Exception.class)
    public CategoryInfo createCategory(CreateCategoryRequest request) {
        log.info("创建分类: {}", request.getName());
        
        // 检查分类名称是否已存在
        if (documentCategoryRepository.existsByName(request.getName())) {
            throw new RuntimeException("分类名称已存在");
        }
        
        DocumentCategory category = new DocumentCategory();
        BeanUtils.copyProperties(request, category);
        category.setIsActive(true);
        category.setCreatedAt(LocalDateTime.now());
        category.setUpdatedAt(LocalDateTime.now());
        
        DocumentCategory savedCategory = documentCategoryRepository.save(category);
        log.info("分类创建成功: {}", savedCategory.getId());
        return convertToInfo(savedCategory);
    }
    
    /**
     * 更新分类
     */
    @Transactional(rollbackFor = Exception.class)
    public CategoryInfo updateCategory(Integer categoryId, UpdateCategoryRequest request) {
        log.info("更新分类: {}", categoryId);
        
        Optional<DocumentCategory> optionalCategory = documentCategoryRepository.findById(categoryId);
        if (optionalCategory.isEmpty()) {
            throw new RuntimeException("分类不存在");
        }
        
        DocumentCategory category = optionalCategory.get();
        
        // 检查新名称是否与其他分类冲突
        if (!category.getName().equals(request.getName())) {
            if (documentCategoryRepository.existsByNameAndIdNot(request.getName(), categoryId)) {
                throw new RuntimeException("分类名称已存在");
            }
        }
        
        BeanUtils.copyProperties(request, category);
        category.setUpdatedAt(LocalDateTime.now());
        
        DocumentCategory savedCategory = documentCategoryRepository.save(category);
        log.info("分类更新成功: {}", categoryId);
        return convertToInfo(savedCategory);
    }
    
    /**
     * 删除分类
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteCategory(Integer categoryId) {
        log.info("删除分类: {}", categoryId);
        
        if (!documentCategoryRepository.existsById(categoryId)) {
            throw new RuntimeException("分类不存在");
        }
        
        // 检查是否有文档使用此分类
        Long documentCount = communityDocumentRepository.countByCategoryId(categoryId);
        if (documentCount > 0) {
            throw new RuntimeException("该分类下还有文档，无法删除");
        }
        
        documentCategoryRepository.deleteById(categoryId);
        log.info("分类删除成功: {}", categoryId);
    }
    
    /**
     * 获取分类下的文档数量
     */
    public Page<CategoryInfo> getCategoryDocuments(Integer categoryId, int page, int size) {
        log.info("获取分类 {} 下的文档, page: {}, size: {}", categoryId, page, size);
        
        // 这里应该返回分类下的文档列表，暂时返回空页面
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        return new PageImpl<>(new ArrayList<>(), pageable, 0);
    }
    
    /**
     * 转换为CategoryInfo
     */
    private CategoryInfo convertToInfo(DocumentCategory category) {
        CategoryInfo info = new CategoryInfo();
        BeanUtils.copyProperties(category, info);
        
        // 获取分类下的文档数量
        Long documentCount = communityDocumentRepository.countByCategoryId(category.getId());
        info.setDocumentCount(documentCount.intValue());
        
        return info;
    }
}