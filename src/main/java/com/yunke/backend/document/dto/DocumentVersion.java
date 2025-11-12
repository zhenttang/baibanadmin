package com.yunke.backend.document.dto;

import java.time.Instant;

/**
 * 文档版本 DTO
 */
public record DocumentVersion(
    String id,
    String docId,
    String content,
    String userId,
    Instant timestamp,
    String operationType
) {} 