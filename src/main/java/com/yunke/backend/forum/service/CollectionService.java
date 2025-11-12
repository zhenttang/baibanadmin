package com.yunke.backend.forum.service;

import com.yunke.backend.system.domain.entity.EntityCollection;
import com.yunke.backend.forum.domain.entity.ForumPost;
import com.yunke.backend.system.repository.EntityCollectionRepository;
import com.yunke.backend.forum.repository.ForumPostRepository;
import com.yunke.backend.security.AffineUserDetails;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CollectionService {

    private static final String ENTITY_TYPE_POST = "POST";

    private final EntityCollectionRepository entityCollectionRepository;
    private final ForumPostRepository forumPostRepository;

    /**
     * 收藏帖子（幂等）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean collectPost(String postId) {
        String userId = currentUserId();
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("未登录或无法获取用户信息");
        }
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));

        boolean exists = entityCollectionRepository
                .existsByUserIdAndEntityTypeAndEntityId(userId, ENTITY_TYPE_POST, postId);
        if (exists) {
            return true;
        }

        EntityCollection ec = new EntityCollection();
        ec.setUserId(userId);
        ec.setEntityType(ENTITY_TYPE_POST);
        ec.setEntityId(postId);
        entityCollectionRepository.save(ec);

        Integer count = post.getCollectCount() == null ? 0 : post.getCollectCount();
        post.setCollectCount(count + 1);
        forumPostRepository.save(post);
        return true;
    }

    /**
     * 取消收藏帖子（幂等）
     */
    @Transactional(rollbackFor = Exception.class)
    public boolean uncollectPost(String postId) {
        String userId = currentUserId();
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("未登录或无法获取用户信息");
        }
        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));

        Optional<EntityCollection> opt = entityCollectionRepository
                .findByUserIdAndEntityTypeAndEntityId(userId, ENTITY_TYPE_POST, postId);
        if (opt.isPresent()) {
            entityCollectionRepository.deleteByUserIdAndEntityTypeAndEntityId(userId, ENTITY_TYPE_POST, postId);
            Integer count = post.getCollectCount() == null ? 0 : post.getCollectCount();
            post.setCollectCount(Math.max(0, count - 1));
            forumPostRepository.save(post);
        }
        return true;
    }

    /**
     * 检查是否收藏帖子
     */
    @Transactional(readOnly = true)
    public boolean isPostCollected(String postId) {
        String userId = currentUserId();
        if (userId == null || userId.isBlank()) {
            return false;
        }
        return entityCollectionRepository
                .existsByUserIdAndEntityTypeAndEntityId(userId, ENTITY_TYPE_POST, postId);
    }

    /**
     * 分页查询我的帖子收藏列表
     */
    @Transactional(readOnly = true)
    public Page<ForumPost> listMyCollectedPosts(int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        Pageable pageable = PageRequest.of(page, Math.min(100, size));

        String userId = currentUserId();
        if (userId == null || userId.isBlank()) {
            throw new IllegalStateException("未登录或无法获取用户信息");
        }

        Page<EntityCollection> p = entityCollectionRepository
                .findByUserIdAndEntityTypeOrderByCreatedAtDesc(userId, ENTITY_TYPE_POST, pageable);

        List<String> ids = p.map(EntityCollection::getEntityId).getContent();
        if (ids.isEmpty()) {
            return new PageImpl<>(Collections.emptyList(), pageable, p.getTotalElements());
        }

        // 查出帖子并保持与收藏顺序一致
        List<ForumPost> posts = new ArrayList<>();
        Map<String, ForumPost> postMap = new HashMap<>();
        forumPostRepository.findAllById(ids).forEach(fp -> postMap.put(fp.getId(), fp));
        for (String id : ids) {
            ForumPost fp = postMap.get(id);
            if (fp != null) posts.add(fp);
        }

        return new PageImpl<>(posts, pageable, p.getTotalElements());
    }

    private String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof AffineUserDetails aud) {
            return aud.getUserId();
        }
        return null;
    }
}

