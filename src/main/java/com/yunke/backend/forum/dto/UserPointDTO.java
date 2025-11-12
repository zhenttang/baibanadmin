package com.yunke.backend.forum.dto;

import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class UserPointDTO {
    private Long id;
    private Long userId;
    private String username;
    private Integer totalPoints;
    private Integer level;
    private Integer postCount;
    private Integer replyCount;
    private Integer reputation;
    private LocalDate lastSignInDate;
    private Integer continuousSignInDays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

