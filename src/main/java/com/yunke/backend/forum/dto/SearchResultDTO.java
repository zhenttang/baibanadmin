package com.yunke.backend.forum.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SearchResultDTO {
    private String type; // "POST" 或 "FORUM"
    private String id;
    private String title;
    private String content; // 帖子内容摘要或板块描述
    private String highlight; // 高亮的匹配片段
    private Long forumId; // 所属板块（仅POST类型有）
    private String forumName; // 所属板块名（仅POST类型有）
    private Long authorId; // 作者ID（仅POST类型有）
    private String authorName; // 作者名（仅POST类型有）
    private Integer replyCount; // 回复数（仅POST类型有）
    private LocalDateTime createdAt;
}

