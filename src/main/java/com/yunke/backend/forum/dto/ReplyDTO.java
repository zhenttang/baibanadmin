package com.yunke.backend.forum.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ReplyDTO {
    private Long id;
    private String postId;
    private Long userId;
    private String username;
    private Integer floor; // 楼层号
    private Long parentId; // 父回复ID（用于嵌套回复）
    private String content;
    private Integer likeCount;
    private Boolean isBestAnswer;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

