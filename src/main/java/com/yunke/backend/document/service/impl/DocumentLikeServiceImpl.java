package com.yunke.backend.document.service.impl;

import com.yunke.backend.community.dto.response.LikeStatusResponse;
import com.yunke.backend.community.dto.response.UserLikeInfo;
import com.yunke.backend.document.domain.entity.DocumentLike;
import com.yunke.backend.document.repository.DocumentLikeRepository;
import com.yunke.backend.user.domain.entity.User;

import com.yunke.backend.user.repository.UserRepository;
import com.yunke.backend.document.service.DocumentLikeService;
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
 * 文档点赞服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentLikeServiceImpl implements DocumentLikeService {

    private final DocumentLikeRepository documentLikeRepository;
    private final UserRepository userRepository;
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void likeDocument(String documentId, String userId) {
        log.info("用户 {} 点赞文档 {}", userId, documentId);
        
        // 1. 检查是否已点赞
        boolean alreadyLiked = documentLikeRepository.existsByDocumentIdAndUserId(documentId, userId);
        if (alreadyLiked) {
            log.warn("用户 {} 已经点赞过文档 {}", userId, documentId);
            return;
        }
        
        // 2. 插入点赞记录
        DocumentLike documentLike = new DocumentLike();
        documentLike.setDocumentId(documentId);
        documentLike.setUserId(userId);
        documentLike.setCreatedAt(LocalDateTime.now());
        
        documentLikeRepository.save(documentLike);
        
        log.info("用户 {} 点赞文档 {} 成功", userId, documentId);
    }
    
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unlikeDocument(String documentId, String userId) {
        log.info("用户 {} 取消点赞文档 {}", userId, documentId);
        
        // 1. 检查是否已点赞
        boolean isLiked = documentLikeRepository.existsByDocumentIdAndUserId(documentId, userId);
        if (!isLiked) {
            log.warn("用户 {} 未点赞文档 {}，无法取消点赞", userId, documentId);
            return;
        }
        
        // 2. 删除点赞记录
        documentLikeRepository.deleteByDocumentIdAndUserId(documentId, userId);
        
        log.info("用户 {} 取消点赞文档 {} 成功", userId, documentId);
    }
    
    @Override
    public LikeStatusResponse getLikeStatus(String documentId, String userId) {
        // 1. 检查当前用户是否已点赞
        boolean isLiked = false;
        if (userId != null) {
            isLiked = documentLikeRepository.existsByDocumentIdAndUserId(documentId, userId);
        }
        
        // 2. 获取总点赞数
        Long totalLikes = documentLikeRepository.countByDocumentId(documentId);
        
        LikeStatusResponse response = new LikeStatusResponse();
        response.setIsLiked(isLiked);
        response.setLikeCount(totalLikes != null ? totalLikes.intValue() : 0);
        return response;
    }
    
    @Override
    public Page<UserLikeInfo> getDocumentLikes(String documentId, int page, int size) {
        try {
            List<String> userIds = documentLikeRepository.findUserIdsByDocumentId(documentId);

            Pageable pageable = PageRequest.of(Math.max(0, page - 1), Math.max(1, size));
            int start = (int) pageable.getOffset();
            int end = Math.min(start + size, userIds.size());

            List<UserLikeInfo> userLikeInfos = new ArrayList<>();
            if (start < userIds.size()) {
                List<String> pageUserIds = userIds.subList(start, end);
                for (String userId : pageUserIds) {
                    User user = userRepository.findById(userId).orElse(null);
                    if (user != null) {
                        UserLikeInfo userInfo = new UserLikeInfo();
                        userInfo.setUserId(user.getId());
                        userInfo.setUserName(user.getName());
                        userInfo.setAvatar(user.getAvatarUrl() != null ? user.getAvatarUrl() : "/avatars/default.png");
                        userLikeInfos.add(userInfo);
                    }
                }
            }

            return new PageImpl<>(userLikeInfos, pageable, userIds.size());

        } catch (Exception e) {
            log.error("获取文档点赞用户列表失败", e);
            return new PageImpl<>(new ArrayList<>(), PageRequest.of(0, size), 0);
        }
    }
}