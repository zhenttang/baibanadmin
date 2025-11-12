package com.yunke.backend.community.service;


import com.yunke.backend.community.dto.community.TagInfo;
import com.yunke.backend.community.dto.community.CreateTagRequest;
import com.yunke.backend.document.domain.entity.DocumentTag;
import com.yunke.backend.document.domain.entity.DocumentTagRelation;
import com.yunke.backend.document.repository.DocumentTagRepository;
import com.yunke.backend.document.repository.DocumentTagRelationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

/**
 * 文档标签服务类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentTagService {

    private final DocumentTagRepository documentTagRepository;
    private final DocumentTagRelationRepository documentTagRelationRepository;

    /**
     * 获取标签分页列表
     */
    public Page<TagInfo> getAllTags(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));
        
        Page<DocumentTag> tagPage = documentTagRepository.findAll(pageable);
        
        // 转换为TagInfo列表
        List<TagInfo> tagInfos = new ArrayList<>();
        for (DocumentTag tag : tagPage.getContent()) {
            TagInfo tagInfo = new TagInfo();
            tagInfo.setId(tag.getId());
            tagInfo.setName(tag.getName());
            tagInfo.setColor(tag.getColor());
            tagInfo.setUsageCount(tag.getUseCount());
            tagInfos.add(tagInfo);
        }
        
        return new PageImpl<>(tagInfos, pageable, tagPage.getTotalElements());
    }

    /**
     * 创建标签
     */
    @Transactional(rollbackFor = Exception.class)
    public TagInfo createTag(CreateTagRequest request) {
        log.info("创建标签: {}", request.getName());
        
        // 检查标签名称是否已存在
        if (documentTagRepository.existsByNameAndIdNot(request.getName(), null)) {
            throw new RuntimeException("标签名称已存在");
        }
        
        // 创建标签
        DocumentTag tag = new DocumentTag();
        tag.setName(request.getName());
        tag.setColor(request.getColor() != null ? request.getColor() : "#1890ff");
        tag.setUseCount(0);
        tag.setCreatedAt(LocalDateTime.now());
        
        DocumentTag savedTag = documentTagRepository.save(tag);
        
        // 转换为TagInfo
        TagInfo tagInfo = new TagInfo();
        tagInfo.setId(savedTag.getId());
        tagInfo.setName(savedTag.getName());
        tagInfo.setColor(savedTag.getColor());
        tagInfo.setUsageCount(savedTag.getUseCount());
        
        log.info("标签创建成功: {}", savedTag.getId());
        return tagInfo;
    }

    /**
     * 更新标签
     */
    @Transactional(rollbackFor = Exception.class)
    public TagInfo updateTag(Integer id, CreateTagRequest request) {
        log.info("更新标签: {}", id);
        
        Optional<DocumentTag> optionalTag = documentTagRepository.findById(id);
        if (optionalTag.isEmpty()) {
            throw new RuntimeException("标签不存在");
        }
        
        DocumentTag tag = optionalTag.get();
        
        // 检查标签名称是否已存在（排除当前标签）
        if (documentTagRepository.existsByNameAndIdNot(request.getName(), id)) {
            throw new RuntimeException("标签名称已存在");
        }
        
        // 更新标签信息
        tag.setName(request.getName());
        if (request.getColor() != null) {
            tag.setColor(request.getColor());
        }
        
        DocumentTag savedTag = documentTagRepository.save(tag);
        
        // 转换为TagInfo
        TagInfo tagInfo = new TagInfo();
        tagInfo.setId(savedTag.getId());
        tagInfo.setName(savedTag.getName());
        tagInfo.setColor(savedTag.getColor());
        tagInfo.setUsageCount(savedTag.getUseCount());
        
        log.info("标签更新成功: {}", id);
        return tagInfo;
    }

    /**
     * 删除标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteTag(Integer id) {
        log.info("删除标签: {}", id);
        
        if (!documentTagRepository.existsById(id)) {
            throw new RuntimeException("标签不存在");
        }
        
        // 删除标签关联关系
        documentTagRelationRepository.deleteByTagId(id);
        
        // 删除标签
        documentTagRepository.deleteById(id);
        
        log.info("标签删除成功: {}", id);
    }

    /**
     * 获取热门标签
     */
    public List<TagInfo> getHotTags(Integer limit) {
        List<DocumentTag> tags = documentTagRepository.findTopPopularTags(limit);
        
        List<TagInfo> tagInfos = new ArrayList<>();
        for (DocumentTag tag : tags) {
            TagInfo tagInfo = new TagInfo();
            tagInfo.setId(tag.getId());
            tagInfo.setName(tag.getName());
            tagInfo.setColor(tag.getColor());
            tagInfo.setUsageCount(tag.getUseCount());
            tagInfos.add(tagInfo);
        }
        
        return tagInfos;
    }

    /**
     * 搜索标签
     */
    public List<TagInfo> searchTags(String query) {
        List<DocumentTag> tags = documentTagRepository.findByNameContainingIgnoreCaseOrderByUseCountDesc(query.trim());
        
        List<TagInfo> tagInfos = new ArrayList<>();
        for (DocumentTag tag : tags) {
            TagInfo tagInfo = new TagInfo();
            tagInfo.setId(tag.getId());
            tagInfo.setName(tag.getName());
            tagInfo.setColor(tag.getColor());
            tagInfo.setUsageCount(tag.getUseCount());
            tagInfos.add(tagInfo);
        }
        
        return tagInfos;
    }

    /**
     * 为文档添加标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void addDocumentTags(String documentId, List<String> tagNames, String userId) {
        log.info("为文档 {} 添加标签: {}", documentId, tagNames);
        
        for (String tagName : tagNames) {
            // 查找或创建标签
            Optional<DocumentTag> optionalTag = documentTagRepository.findByName(tagName);
            DocumentTag tag;
            if (optionalTag.isPresent()) {
                tag = optionalTag.get();
            } else {
                // 创建新标签
                tag = new DocumentTag();
                tag.setName(tagName);
                tag.setColor("#1890ff");
                tag.setUseCount(0);
                tag.setCreatedAt(LocalDateTime.now());
                tag = documentTagRepository.save(tag);
            }
            
            // 添加文档标签关联
            addTagToDocument(documentId, tag.getId());
        }
        
        log.info("文档 {} 添加标签完成", documentId);
    }

    /**
     * 移除文档标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeDocumentTags(String documentId, List<Integer> tagIds, String userId) {
        log.info("移除文档 {} 的标签: {}", documentId, tagIds);
        
        for (Integer tagId : tagIds) {
            removeTagFromDocument(documentId, tagId);
        }
        
        log.info("文档 {} 移除标签完成", documentId);
    }

    /**
     * 获取文档的标签列表
     */
    public List<TagInfo> getDocumentTags(String documentId) {
        List<DocumentTagRelation> relations = documentTagRelationRepository.findByDocumentId(documentId);
        List<TagInfo> tagInfos = new ArrayList<>();
        
        for (DocumentTagRelation relation : relations) {
            Optional<DocumentTag> optionalTag = documentTagRepository.findById(relation.getTagId());
            if (optionalTag.isPresent()) {
                DocumentTag tag = optionalTag.get();
                TagInfo tagInfo = new TagInfo();
                tagInfo.setId(tag.getId());
                tagInfo.setName(tag.getName());
                tagInfo.setColor(tag.getColor());
                tagInfo.setUsageCount(tag.getUseCount());
                tagInfos.add(tagInfo);
            }
        }
        
        return tagInfos;
    }

    /**
     * 为文档添加标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void addTagToDocument(String documentId, Integer tagId) {
        // 检查是否已经关联
        if (documentTagRelationRepository.existsByDocumentIdAndTagId(documentId, tagId)) {
            return;
        }
        
        // 创建关联关系
        DocumentTagRelation relation = new DocumentTagRelation();
        relation.setDocumentId(documentId);
        relation.setTagId(tagId);
        relation.setCreatedAt(LocalDateTime.now());
        
        documentTagRelationRepository.save(relation);
        
        // 增加标签使用次数
        documentTagRepository.incrementUsageCount(tagId);
    }

    /**
     * 从文档移除标签
     */
    @Transactional(rollbackFor = Exception.class)
    public void removeTagFromDocument(String documentId, Integer tagId) {
        // 删除关联关系
        documentTagRelationRepository.deleteByDocumentIdAndTagId(documentId, tagId);
        
        // 减少标签使用次数
        documentTagRepository.decrementUsageCount(tagId);
    }
}