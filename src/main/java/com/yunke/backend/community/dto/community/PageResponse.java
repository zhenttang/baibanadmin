package com.yunke.backend.community.dto.community;

import lombok.Data;
import java.util.List;

/**
 * 分页响应格式
 */
@Data
public class PageResponse<T> {
    private List<T> content;
    private int number;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean first;
    private boolean last;
    private boolean empty;
}