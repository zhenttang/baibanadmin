package com.yunke.backend.forum.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ForumDTO {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private String icon;
    private String banner;
    private Long parentId;
    private Integer displayOrder;
    private Integer postCount;
    private Integer topicCount;
    private Boolean isActive;
    private Boolean isPrivate;
    private String announcement;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ForumDTO> children = new ArrayList<>();
}

