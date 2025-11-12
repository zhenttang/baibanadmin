package com.yunke.backend.search.dto;

import com.yunke.backend.system.dto.SearchNodeDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregateBucketDto {
    private String key;
    private Long docCount;
    private List<SearchNodeDto> hits;
}