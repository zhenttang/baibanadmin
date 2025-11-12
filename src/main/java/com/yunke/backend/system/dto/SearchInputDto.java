package com.yunke.backend.system.dto;

import com.yunke.backend.search.enums.SearchTable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchInputDto {
    private SearchTable table;
    private SearchQueryDto query;
    private SearchOptionsDto options;
}