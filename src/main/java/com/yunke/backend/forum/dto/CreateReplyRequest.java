package com.yunke.backend.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateReplyRequest {
    @NotNull
    private String postId;
    
    @NotBlank
    private String content;
    
    private Long parentId; // 可选，回复某条评论时使用
}

