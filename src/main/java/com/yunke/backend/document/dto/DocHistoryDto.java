package com.yunke.backend.document.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.yunke.backend.user.dto.UserDto;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocHistoryDto {
    private String id;
    private String workspaceId;
    private String docId;
    private String pageDocId;  // 前端期望的字段名
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private String timestamp;  // 改为 String，前端期望 ISO 字符串
    
    private String version;    // 版本号
    private String title;      // 文档标题
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private String createdAt;  // 创建时间
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private String updatedAt;  // 更新时间
    
    private String createdBy;
    private UserDto editor;
    
    // Snapshot information
    private Long blobSize;
    private Boolean hasState;
    private Integer seq;
}