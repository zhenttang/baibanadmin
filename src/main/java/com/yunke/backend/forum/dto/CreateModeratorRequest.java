package com.yunke.backend.forum.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateModeratorRequest {
    @NotNull
    private Long forumId;

    @NotBlank
    private String userId;

    private String userName;

    @NotNull
    private String role = "DEPUTY";

    private String permissions = "{}";
}

