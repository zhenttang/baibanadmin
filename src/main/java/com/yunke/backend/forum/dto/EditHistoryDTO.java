package com.yunke.backend.forum.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EditHistoryDTO {
    private Long id;
    private String postId;
    private String oldTitle;
    private String oldContent;
    private String editorId;
    private String editorName;
    private LocalDateTime editedAt;
}

