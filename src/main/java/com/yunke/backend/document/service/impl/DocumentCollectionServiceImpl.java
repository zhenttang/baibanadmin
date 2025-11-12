package com.yunke.backend.document.service.impl;


import com.yunke.backend.community.domain.entity.CommunityDocument;
import com.yunke.backend.community.dto.request.CollectRequest;
import com.yunke.backend.community.dto.response.CollectedDocumentInfo;
import com.yunke.backend.document.domain.entity.DocumentCollection;
import com.yunke.backend.community.repository.CommunityDocumentRepository;

import com.yunke.backend.document.repository.DocumentCollectionRepository;
import com.yunke.backend.document.service.DocumentCollectionService;
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

/**
 * 文档收藏服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentCollectionServiceImpl implements DocumentCollectionService {

    private final DocumentCollectionRepository documentCollectionRepository;
    private final CommunityDocumentRepository communityDocumentRepository;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void collectDocument(String documentId, String userId, CollectRequest request) {
        log.info("用户 {} 收藏文档 {}", userId, documentId);
        
        // 检查是否已收藏
        if (documentCollectionRepository.existsByDocumentIdAndUserId(documentId, userId)) {
            log.warn("用户 {} 已经收藏过文档 {}", userId, documentId);
            return;
        }
        
        // 创建收藏记录
        DocumentCollection collection = new DocumentCollection();
        collection.setDocumentId(documentId);
        collection.setUserId(userId);
        collection.setCollectionName(request.getCollectionName() != null ? 
            request.getCollectionName() : "默认收藏夹");
        collection.setCreatedAt(LocalDateTime.now());
        
        documentCollectionRepository.save(collection);
        log.info("用户 {} 收藏文档 {} 成功", userId, documentId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void uncollectDocument(String documentId, String userId) {
        log.info("用户 {} 取消收藏文档 {}", userId, documentId);
        
        // 检查收藏关系是否存在
        if (!documentCollectionRepository.existsByDocumentIdAndUserId(documentId, userId)) {
            log.warn("用户 {} 未收藏文档 {}，无法取消收藏", userId, documentId);
            return;
        }
        
        // 删除收藏记录
        documentCollectionRepository.deleteByDocumentIdAndUserId(documentId, userId);
        log.info("用户 {} 取消收藏文档 {} 成功", userId, documentId);
    }
    
    @Override
    public Page<CollectedDocumentInfo> getUserCollections(String userId, int page, int size) {
        try {
            List<DocumentCollection> collections = documentCollectionRepository.findByUserId(userId);

            List<CollectedDocumentInfo> collectedDocs = new ArrayList<>();

            Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, size));
            int start = (int) pageable.getOffset();
            int end = Math.min(start + size, collections.size());

            if (start < collections.size()) {
                List<DocumentCollection> pageCollections = collections.subList(start, end);
                for (DocumentCollection collection : pageCollections) {
                    CommunityDocument document = communityDocumentRepository.findById(collection.getDocumentId()).orElse(null);

                    CollectedDocumentInfo docInfo = new CollectedDocumentInfo();
                    docInfo.setDocumentId(collection.getDocumentId());

                    if (document != null) {
                        docInfo.setTitle(document.getTitle());
                        docInfo.setDescription(document.getDescription());
                    } else {
                        docInfo.setTitle("文档不存在");
                        docInfo.setDescription("该文档已被删除或不可见");
                    }

                    docInfo.setCollectionName(collection.getCollectionName());
                    docInfo.setCollectedAt(collection.getCreatedAt());
                    collectedDocs.add(docInfo);
                }
            }

            return new PageImpl<>(collectedDocs, pageable, collections.size());
        } catch (Exception e) {
            log.error("获取用户收藏列表失败", e);
            return new PageImpl<>(new ArrayList<>(), PageRequest.of(0, size), 0);
        }
    }
    
    @Override
    public boolean isCollected(String documentId, String userId) {
        return documentCollectionRepository.existsByDocumentIdAndUserId(documentId, userId);
    }
    
    @Override
    public Long getDocumentCollectionCount(String documentId) {
        return documentCollectionRepository.countByDocumentId(documentId);
    }
    
    @Override
    public Long getUserCollectionCount(String userId) {
        return documentCollectionRepository.countByUserId(userId);
    }
}