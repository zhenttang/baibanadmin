package com.yunke.backend.forum.service;

import com.yunke.backend.forum.dto.CreateForumRequest;
import com.yunke.backend.forum.dto.ForumDTO;
import com.yunke.backend.forum.dto.ForumStatsDTO;
import com.yunke.backend.forum.dto.UpdateForumRequest;
import com.yunke.backend.forum.domain.entity.Forum;
import com.yunke.backend.forum.repository.ForumRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ForumService {

    private final ForumRepository forumRepository;

    @Transactional(rollbackFor = Exception.class)
    public ForumDTO createForum(CreateForumRequest request) {
        if (forumRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Slug已存在");
        }

        Forum forum = new Forum();
        forum.setName(request.getName());
        forum.setSlug(request.getSlug());
        forum.setDescription(request.getDescription());
        forum.setIcon(request.getIcon());
        forum.setBanner(request.getBanner());
        forum.setParentId(request.getParentId());
        if (request.getDisplayOrder() != null) {
            forum.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsPrivate() != null) {
            forum.setIsPrivate(request.getIsPrivate());
        }
        forum.setAnnouncement(request.getAnnouncement());

        Forum saved = forumRepository.save(forum);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ForumDTO> listForums() {
        List<Forum> forums = forumRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return buildForumTree(forums);
    }

    @Transactional(readOnly = true)
    public ForumDTO getForum(Long id) {
        Forum forum = forumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("板块不存在"));
        return toDTO(forum);
    }

    @Transactional(rollbackFor = Exception.class)
    public ForumDTO updateForum(Long id, UpdateForumRequest request) {
        Forum forum = forumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("板块不存在"));

        if (request.getName() != null) {
            forum.setName(request.getName());
        }
        if (request.getDescription() != null) {
            forum.setDescription(request.getDescription());
        }
        if (request.getIcon() != null) {
            forum.setIcon(request.getIcon());
        }
        if (request.getBanner() != null) {
            forum.setBanner(request.getBanner());
        }
        if (request.getDisplayOrder() != null) {
            forum.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsActive() != null) {
            forum.setIsActive(request.getIsActive());
        }
        if (request.getIsPrivate() != null) {
            forum.setIsPrivate(request.getIsPrivate());
        }
        if (request.getAnnouncement() != null) {
            forum.setAnnouncement(request.getAnnouncement());
        }

        Forum saved = forumRepository.save(forum);
        return toDTO(saved);
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteForum(Long id) {
        Forum forum = forumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("板块不存在"));
        forum.setIsActive(false);
        forumRepository.save(forum);
        return true;
    }

    @Transactional(readOnly = true)
    public ForumStatsDTO getForumStats(Long id) {
        Forum forum = forumRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("板块不存在"));

        ForumStatsDTO stats = new ForumStatsDTO();
        stats.setForumId(forum.getId());
        stats.setPostCount(Optional.ofNullable(forum.getPostCount()).orElse(0));
        stats.setTopicCount(Optional.ofNullable(forum.getTopicCount()).orElse(0));
        stats.setTodayPostCount(0);
        stats.setActiveUserCount(0);
        return stats;
    }

    private List<ForumDTO> buildForumTree(List<Forum> forums) {
        if (forums == null || forums.isEmpty()) {
            return Collections.emptyList();
        }

        // parentId -> children forums
        Map<Long, List<Forum>> childrenMap = new HashMap<>();
        List<Forum> roots = new ArrayList<>();

        for (Forum f : forums) {
            Long pid = f.getParentId();
            if (pid == null) {
                roots.add(f);
            } else {
                childrenMap.computeIfAbsent(pid, k -> new ArrayList<>()).add(f);
            }
        }

        // sort children lists by displayOrder for deterministic ordering
        for (List<Forum> list : childrenMap.values()) {
            list.sort(Comparator.comparingInt(o -> Optional.ofNullable(o.getDisplayOrder()).orElse(0)));
        }

        // build tree recursively from roots
        roots.sort(Comparator.comparingInt(o -> Optional.ofNullable(o.getDisplayOrder()).orElse(0)));
        List<ForumDTO> tree = new ArrayList<>();
        for (Forum root : roots) {
            tree.add(buildNodeRecursive(root, childrenMap));
        }
        return tree;
    }

    private ForumDTO buildNodeRecursive(Forum forum, Map<Long, List<Forum>> childrenMap) {
        ForumDTO dto = toDTO(forum);
        List<Forum> children = childrenMap.getOrDefault(forum.getId(), Collections.emptyList());
        for (Forum child : children) {
            dto.getChildren().add(buildNodeRecursive(child, childrenMap));
        }
        return dto;
    }

    public ForumDTO toDTO(Forum forum) {
        ForumDTO dto = new ForumDTO();
        dto.setId(forum.getId());
        dto.setName(forum.getName());
        dto.setSlug(forum.getSlug());
        dto.setDescription(forum.getDescription());
        dto.setIcon(forum.getIcon());
        dto.setBanner(forum.getBanner());
        dto.setParentId(forum.getParentId());
        dto.setDisplayOrder(forum.getDisplayOrder());
        dto.setPostCount(forum.getPostCount());
        dto.setTopicCount(forum.getTopicCount());
        dto.setIsActive(forum.getIsActive());
        dto.setIsPrivate(forum.getIsPrivate());
        dto.setAnnouncement(forum.getAnnouncement());
        dto.setCreatedAt(forum.getCreatedAt());
        dto.setUpdatedAt(forum.getUpdatedAt());
        dto.setChildren(new ArrayList<>()); // 初始化children，构建树时填充
        return dto;
    }
}
