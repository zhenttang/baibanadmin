package com.yunke.backend.document.dto;

import java.time.Instant;
import java.util.List;

/**
 * 文档状态 DTO
 */
public record DocumentState(
    String docId,
    String content,
    int version,
    Instant lastModified,
    List<ActiveCollaborator> collaborators
) {} 