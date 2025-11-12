package com.yunke.backend.search.dto;

import com.yunke.backend.search.enums.SearchTable;
import com.yunke.backend.system.dto.SearchQueryDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AggregateInputDto {
    private SearchTable table;
    private SearchQueryDto query;
    private String field;
    private AggregateOptionsDto options;
}