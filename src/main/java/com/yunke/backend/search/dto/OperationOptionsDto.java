package com.yunke.backend.search.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OperationOptionsDto {
    private Boolean refresh;
    private String routing;
    private Integer timeout;
}