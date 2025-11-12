package com.yunke.backend.forum.dto;

import lombok.Data;

@Data
public class UpdateForumRequest {
    private String name;
    private String description;
    private String icon;
    private String banner;
    private Integer displayOrder;
    private Boolean isActive;
    private Boolean isPrivate;
    private String announcement;
}

