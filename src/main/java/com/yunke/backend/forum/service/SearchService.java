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
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final ForumPostRepository postRepository;
    private final ForumRepository forumRepository;

    public List<SearchResultDTO> searchAll(SearchRequest request, int page, int size) {
        if (request == null || request.getKeyword() == null || request.getKeyword().isBlank()) {
            return Collections.emptyList();
        }

        String type = request.getType();
        if (type == null || type.isBlank() || "ALL".equalsIgnoreCase(type)) {
            List<SearchResultDTO> results = new ArrayList<>();
            results.addAll(searchPosts(request.getKeyword().trim(), request.getForumId(), page, size));
            results.addAll(searchForums(request.getKeyword().trim()));
            return results;
        }

        if ("POST".equalsIgnoreCase(type)) {
            return searchPosts(request.getKeyword().trim(), request.getForumId(), page, size);
        }
        if ("FORUM".equalsIgnoreCase(type)) {
            return searchForums(request.getKeyword().trim());
        }
        // 未知类型，按ALL处理
        List<SearchResultDTO> results = new ArrayList<>();
        results.addAll(searchPosts(request.getKeyword().trim(), request.getForumId(), page, size));
        results.addAll(searchForums(request.getKeyword().trim()));
        return results;
    }

    public List<SearchResultDTO> searchPosts(String keyword, Long forumId, int page, int size) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        int p = Math.max(0, page);
        int s = Math.max(1, Math.min(100, size));
        Pageable pageable = PageRequest.of(p, s);

        // 使用现有仓库方法（带状态），然后必要时内存中过滤forumId
        Page<ForumPost> pageData = postRepository.searchPosts(keyword, ForumPost.PostStatus.NORMAL, pageable);

        List<ForumPost> posts = pageData.getContent();
        if (forumId != null) {
            posts = posts.stream().filter(po -> forumId.equals(po.getForumId())).collect(Collectors.toList());
        }

        String kw = keyword;
        return posts.stream()
                .map(this::toSearchResultDTO)
                .peek(dto -> {
                    // 内容摘要：最多150字符
                    if (dto.getContent() != null && dto.getContent().length() > 150) {
                        dto.setContent(dto.getContent().substring(0, 150));
                    }
                    // 高亮：优先标题，否则内容；每侧约25字符
                    String base = dto.getTitle() != null && containsIgnoreCase(dto.getTitle(), kw)
                            ? dto.getTitle() : dto.getContent();
                    dto.setHighlight(extractHighlight(base, kw, 50));
                    dto.setType("POST");
                })
                .collect(Collectors.toList());
    }

    public List<SearchResultDTO> searchForums(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return Collections.emptyList();
        }
        String kwLower = keyword.toLowerCase(Locale.ROOT);
        List<Forum> forums = forumRepository.findAll();
        return forums.stream()
                .filter(f -> {
                    String name = f.getName() == null ? "" : f.getName();
                    String desc = f.getDescription() == null ? "" : f.getDescription();
                    return name.toLowerCase(Locale.ROOT).contains(kwLower)
                            || desc.toLowerCase(Locale.ROOT).contains(kwLower);
                })
                .map(this::toSearchResultDTO)
                .peek(dto -> {
                    dto.setType("FORUM");
                    // 高亮：优先名称，否则描述
                    String base = containsIgnoreCase(dto.getTitle(), keyword) ? dto.getTitle() : dto.getContent();
                    dto.setHighlight(extractHighlight(base, keyword, 50));
                })
                .collect(Collectors.toList());
    }

    private String extractHighlight(String text, String keyword, int contextLength) {
        if (text == null || text.isBlank() || keyword == null || keyword.isBlank()) {
            return null;
        }
        String src = text;
        String lower = src.toLowerCase(Locale.ROOT);
        String kw = keyword.toLowerCase(Locale.ROOT);
        int idx = lower.indexOf(kw);
        if (idx < 0) {
            return null;
        }
        int half = Math.max(0, contextLength / 2);
        int start = Math.max(0, idx - half);
        int end = Math.min(src.length(), idx + kw.length() + half);
        StringBuilder sb = new StringBuilder();
        if (start > 0) sb.append("...");
        sb.append(src, start, end);
        if (end < src.length()) sb.append("...");
        return sb.toString();
    }

    private SearchResultDTO toSearchResultDTO(ForumPost post) {
        SearchResultDTO dto = new SearchResultDTO();
        dto.setType("POST");
        dto.setId(post.getId());
        dto.setTitle(post.getTitle());
        dto.setContent(post.getContent());
        dto.setForumId(post.getForumId());
        dto.setForumName(post.getForumName());
        dto.setAuthorName(post.getUserName());
        // 将字符串userId尽量转换为Long
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

    private SearchResultDTO toSearchResultDTO(Forum forum) {
        SearchResultDTO dto = new SearchResultDTO();
        dto.setType("FORUM");
        dto.setId(forum.getId() == null ? null : String.valueOf(forum.getId()));
        dto.setTitle(forum.getName());
        dto.setContent(forum.getDescription());
        dto.setCreatedAt(forum.getCreatedAt());
        return dto;
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        if (text == null || keyword == null) return false;
        return text.toLowerCase(Locale.ROOT).contains(keyword.toLowerCase(Locale.ROOT));
    }
}

