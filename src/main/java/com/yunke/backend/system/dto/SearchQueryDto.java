package com.yunke.backend.system.dto;

import com.yunke.backend.search.enums.SearchQueryOccur;
import com.yunke.backend.search.enums.SearchQueryType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchQueryDto {
    private SearchQueryType type;
    private String field;
    private String match;
    private SearchQueryDto query;
    private List<SearchQueryDto> queries;
    private SearchQueryOccur occur;
    private Double boost;
}