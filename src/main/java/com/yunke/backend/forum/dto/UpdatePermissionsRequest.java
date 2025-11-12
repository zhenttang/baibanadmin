package com.yunke.backend.forum.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class UpdatePermissionsRequest {
    @NotEmpty
    private List<String> permissions;
}

