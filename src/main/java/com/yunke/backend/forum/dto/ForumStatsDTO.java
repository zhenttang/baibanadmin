package com.yunke.backend.forum.dto;

import lombok.Data;

@Data
public class ForumStatsDTO {
    private Long forumId;
    private Integer postCount;
    private Integer topicCount;
    private Integer todayPostCount;
    private Integer activeUserCount;
}
