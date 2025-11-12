package com.yunke.backend.forum.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SearchRequest {
    @NotBlank
    private String keyword; // 搜索关键词

    private String type; // 搜索类型：POST, FORUM, ALL（默认ALL）

    private Long forumId; // 限定板块搜索（可选）
}

