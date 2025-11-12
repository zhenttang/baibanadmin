package com.yunke.backend.common.dto;

import java.time.Instant;
import java.util.Map;

/**
 * 操作 DTO
 */
public record Operation(
    String id,
    String docId,
    String userId,
    String type,
    Map<String, Object> data,
    Instant timestamp
) {} 