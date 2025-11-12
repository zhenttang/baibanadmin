package com.yunke.backend.forum.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;

@Data
public class CreateForumRequest {
    @NotBlank
    private String name;

    @NotBlank
    private String slug;

    private String description;
    private String icon;
    private String banner;
    private Long parentId;
    private Integer displayOrder;
    private Boolean isPrivate;
    private String announcement;
}

