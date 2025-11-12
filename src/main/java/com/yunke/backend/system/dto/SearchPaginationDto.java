package com.yunke.backend.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchPaginationDto {
    private Integer skip;
    private Integer limit;
    private String cursor;
}