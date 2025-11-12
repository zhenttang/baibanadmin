package com.yunke.backend.forum.service;

import com.yunke.backend.community.dto.community.TagInfo;
import com.yunke.backend.document.domain.entity.DocumentTag;
import com.yunke.backend.document.domain.entity.DocumentTagRelation;
import com.yunke.backend.forum.domain.entity.ForumPost;
import com.yunke.backend.document.repository.DocumentTagRelationRepository;
import com.yunke.backend.document.repository.DocumentTagRepository;
import com.yunke.backend.forum.repository.ForumPostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostTagService {

    private static final String ENTITY_POST = "POST";

    private final DocumentTagRepository documentTagRepository;
    private final DocumentTagRelationRepository relationRepository;
    private final ForumPostRepository forumPostRepository;

    /**
     * 解析并为帖子添加标签（自动创建不存在的标签）
     */
    @Transactional(rollbackFor = Exception.class)
    public void addTagsToPost(String postId, String tagsString) {
        if (postId == null || postId.isBlank()) return;
        List<String> tagNames = parseTagNames(tagsString);
        if (tagNames.isEmpty()) return;

        for (String name : tagNames) {
            DocumentTag tag = documentTagRepository.findByName(name).orElse(null);
            if (tag == null) {
                tag = new DocumentTag();
                tag.setName(name);
                tag.setSlug(generateUniqueSlug(name));
                // 保持与现有风格一致，未提供则默认色
                if (tag.getColor() == null) {
                    tag.setColor("#1890ff");
                }
                tag.setUseCount(0);
                tag.setCreatedAt(LocalDateTime.now());
                tag = documentTagRepository.save(tag);
            }

            if (!relationRepository.existsByDocumentIdAndTagIdAndEntityType(postId, tag.getId(), ENTITY_POST)) {
                DocumentTagRelation rel = new DocumentTagRelation();
                rel.setDocumentId(postId);
                rel.setTagId(tag.getId());
                rel.setEntityType(ENTITY_POST);
                rel.setCreatedAt(LocalDateTime.now());
                relationRepository.save(rel);

                // 增加使用次数（直接更新并保存，避免自定义@Query缺少@Modifying的问题）
                Integer cnt = Optional.ofNullable(tag.getUseCount()).orElse(0);
                tag.setUseCount(cnt + 1);
                documentTagRepository.save(tag);
            }
        }
    }

    /** 获取帖子标签列表 */
    @Transactional(readOnly = true)
    public List<TagInfo> getPostTags(String postId) {
        if (postId == null || postId.isBlank()) return Collections.emptyList();
        List<DocumentTagRelation> relations = relationRepository.findByDocumentIdAndEntityType(postId, ENTITY_POST);
        if (relations.isEmpty()) return Collections.emptyList();

        List<Integer> tagIds = relations.stream().map(DocumentTagRelation::getTagId).toList();
        List<DocumentTag> tags = documentTagRepository.findAllById(tagIds);
        return tags.stream().map(this::toTagInfo).collect(Collectors.toList());
    }

    /** 按标签ID分页查询帖子 */
    @Transactional(readOnly = true)
    public Page<ForumPost> findPostsByTag(Integer tagId, int page, int size) {
        if (tagId == null) return Page.empty();
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, Math.min(size, 100)));
        Page<String> idPage = relationRepository.findDocumentIdsByTagIdAndEntityType(tagId, ENTITY_POST, pageable);
        List<String> postIds = idPage.getContent();
        if (postIds.isEmpty()) return new PageImpl<>(Collections.emptyList(), pageable, idPage.getTotalElements());

        List<ForumPost> posts = new ArrayList<>();
        forumPostRepository.findAllById(postIds).forEach(posts::add);
        // 仅返回正常状态的帖子
        List<ForumPost> filtered = posts.stream()
                .filter(p -> p.getStatus() == ForumPost.PostStatus.NORMAL)
                .collect(Collectors.toList());
        return new PageImpl<>(filtered, pageable, idPage.getTotalElements());
    }

    /** 热门标签TOP N（默认20） */
    @Transactional(readOnly = true)
    public List<TagInfo> getPopularTagsTopN(int limit) {
        int n = limit <= 0 ? 20 : Math.min(limit, 100);
        Pageable pageable = PageRequest.of(0, n, Sort.by(Sort.Direction.DESC, "useCount"));
        return documentTagRepository.findAll(pageable).getContent().stream()
                .map(this::toTagInfo)
                .collect(Collectors.toList());
    }

    // Helper: 解析标签字符串
    private List<String> parseTagNames(String tagsString) {
        if (tagsString == null) return Collections.emptyList();
        String normalized = tagsString.replace('，', ',');
        String[] parts = normalized.split(",");
        List<String> names = new ArrayList<>();
        for (String p : parts) {
            String s = p == null ? null : p.trim();
            if (s != null && !s.isEmpty()) {
                names.add(s);
            }
        }
        return names;
    }

    // Helper: 生成唯一slug
    private String generateUniqueSlug(String name) {
        String base = slugify(name);
        if (base.isBlank()) {
            base = "tag-" + Integer.toUnsignedString(Objects.hashCode(name), 36);
        }
        String candidate = base;
        int i = 1;
        while (documentTagRepository.existsBySlug(candidate)) {
            candidate = base + "-" + i;
            i++;
        }
        return candidate;
    }

    private String slugify(String input) {
        if (input == null) return "";
        String s = input.trim().toLowerCase(Locale.ROOT);
        // 非字母数字替换为-
        s = s.replaceAll("[^a-z0-9]+", "-");
        // 去掉首尾-
        s = s.replaceAll("^-+", "").replaceAll("-+$", "");
        return s;
    }

    private TagInfo toTagInfo(DocumentTag tag) {
        TagInfo info = new TagInfo();
        info.setId(tag.getId());
        info.setName(tag.getName());
        info.setColor(tag.getColor());
        info.setUsageCount(tag.getUseCount());
        info.setCreatedAt(tag.getCreatedAt());
        return info;
    }
}

