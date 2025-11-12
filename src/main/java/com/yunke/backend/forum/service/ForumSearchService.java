package com.yunke.backend.forum.service;

import com.yunke.backend.forum.dto.SearchRequest;
import com.yunke.backend.forum.dto.SearchResultDTO;
import com.yunke.backend.forum.domain.entity.Forum;
import com.yunke.backend.forum.domain.entity.ForumPost;
import com.yunke.backend.forum.repository.ForumPostRepository;
import com.yunke.backend.forum.repository.ForumRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ForumSearchService {

    private final ForumPostRepository postRepository;
    private final ForumRepository forumRepository;

    // 1. 综合搜索
    @Transactional(readOnly = true)
    public List<SearchResultDTO> searchAll(SearchRequest request, int page, int size) {
        String type = request.getType() == null ? "ALL" : request.getType().toUpperCase(Locale.ROOT);
        List<SearchResultDTO> results = new ArrayList<>();

        if ("POST".equals(type) || "ALL".equals(type)) {
            results.addAll(searchPosts(request.getKeyword(), request.getForumId(), page, size));
        }

        if ("FORUM".equals(type) || "ALL".equals(type)) {
            results.addAll(searchForums(request.getKeyword()));
        }

        return results;
    }

    // 2. 搜索帖子（支持板块限定）
    @Transactional(readOnly = true)
    public List<SearchResultDTO> searchPosts(String keyword, Long forumId, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(0, page), Math.max(1, size));

        Page<ForumPost> pageData = postRepository.searchPosts(keyword, ForumPost.PostStatus.NORMAL, pageable);
        List<ForumPost> posts = pageData.getContent();

        if (forumId != null) {
            posts = posts.stream()
                    .filter(p -> forumId.equals(p.getForumId()))
                    .collect(Collectors.toList());
        }

        return posts.stream()
                .map(post -> toSearchResultDTO(post, keyword))
                .collect(Collectors.toList());
    }

    // 3. 搜索板块
    @Transactional(readOnly = true)
    public List<SearchResultDTO> searchForums(String keyword) {
        List<Forum> forums = forumRepository.findAll();
        final String keywordNormalized = (keyword == null) ? "" : keyword;
        final String kwLower = keywordNormalized.toLowerCase(Locale.ROOT);

        return forums.stream()
                .filter(f ->
                        (f.getName() != null && f.getName().toLowerCase(Locale.ROOT).contains(kwLower)) ||
                        (f.getDescription() != null && f.getDescription().toLowerCase(Locale.ROOT).contains(kwLower))
                )
                .map(f -> toSearchResultDTO(f, keywordNormalized))
                .collect(Collectors.toList());
    }

    // 4. 提取高亮片段（约50字符上下文）
    private String extractHighlight(String text, String keyword, int contextLength) {
        if (text == null || keyword == null) {
            return "";
        }

        // 使用不区分大小写的匹配定位
        String lower = text.toLowerCase(Locale.ROOT);
        String kw = keyword.toLowerCase(Locale.ROOT);
        int index = lower.indexOf(kw);
        if (index == -1) {
            return text.length() > 50 ? text.substring(0, 50) + "..." : text;
        }

        int start = Math.max(0, index - contextLength / 2);
        int end = Math.min(text.length(), index + keyword.length() + contextLength / 2);

        String highlight = text.substring(start, end);
        if (start > 0) {
            highlight = "..." + highlight;
        }
        if (end < text.length()) {
            highlight = highlight + "...";
        }

        return highlight;
    }

    // 5. 转换帖子为搜索结果
    private SearchResultDTO toSearchResultDTO(ForumPost post, String keyword) {
        SearchResultDTO dto = new SearchResultDTO();
        dto.setType("POST");
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());

        // 内容摘要（最多150字符）
        String content = post.getContent();
        if (content != null && content.length() > 150) {
            content = content.substring(0, 150) + "...";
        }
        dto.setContent(content);

        // 高亮片段（从title或content中提取）
        String highlight = "";
        if (post.getTitle() != null && containsIgnoreCase(post.getTitle(), keyword)) {
            highlight = extractHighlight(post.getTitle(), keyword, 50);
        } else if (post.getContent() != null) {
            highlight = extractHighlight(post.getContent(), keyword, 50);
        }
        dto.setHighlight(highlight);

        dto.setForumId(post.getForumId());
        dto.setForumName(post.getForumName());
        dto.setAuthorName(post.getUserName());
        // 尝试将字符串userId转换为Long
        try {
            if (post.getUserId() != null) {
                dto.setAuthorId(Long.parseLong(post.getUserId()));
            }
        } catch (NumberFormatException ignored) {
            dto.setAuthorId(null);
        }
        dto.setReplyCount(post.getReplyCount());
        dto.setCreatedAt(post.getCreatedAt());

        return dto;
    }

    // 6. 转换板块为搜索结果
    private SearchResultDTO toSearchResultDTO(Forum forum, String keyword) {
        SearchResultDTO dto = new SearchResultDTO();
        dto.setType("FORUM");
        dto.setId(forum.getId() == null ? null : forum.getId().toString());
        dto.setTitle(forum.getName());
        dto.setContent(forum.getDescription());

        // 高亮片段
        String highlight = "";
        if (forum.getName() != null && containsIgnoreCase(forum.getName(), keyword)) {
            highlight = extractHighlight(forum.getName(), keyword, 50);
        } else if (forum.getDescription() != null) {
            highlight = extractHighlight(forum.getDescription(), keyword, 50);
        }
        dto.setHighlight(highlight);

        dto.setCreatedAt(forum.getCreatedAt());

        return dto;
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        if (text == null || keyword == null) return false;
        return text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }
}
