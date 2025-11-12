package com.yunke.backend.document.dto;

import com.yunke.backend.user.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocVersionStatsDto {
    private String workspaceId;
    private String docId;
    private Long totalSnapshots;
    private Long totalUpdates;
    private Long totalSize; // total size in bytes
    private LocalDateTime firstVersion;
    private LocalDateTime lastVersion;
    private String lastEditor;
    private UserDto lastEditorUser;
}