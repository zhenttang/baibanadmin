package com.yunke.backend.document.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DuplicateFileDto {
    private String hash;
    private Long size;
    private Integer count;
    private List<FileLocation> locations;
    private Long totalWastedSpace;
    private String fileType;
    private LocalDateTime firstSeen;
    private LocalDateTime lastSeen;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FileLocation {
        private String path;
        private String name;
        private LocalDateTime lastModified;
        private String workspaceId;
        private String documentId;
    }
}
