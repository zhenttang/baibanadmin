package com.yunke.backend.forum.dto;

import lombok.Data;

@Data
public class CreateDraftRequest {
    // 若提供id则为更新草稿，否则为新建
    private Long id;

    private Long forumId;
    private String title;
    private String content;
}

