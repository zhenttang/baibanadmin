package com.yunke.backend.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Data
public class CreatePostRequest {
    @NotNull
    private Long forumId;
    
    @NotBlank
    private String title;
    
    @NotBlank
    private String content;
    
    // 内容类型：markdown/html/plain 等；默认使用服务层回退为 markdown
    private String contentType;
    
    private String tags; // 逗号分隔的标签
}
