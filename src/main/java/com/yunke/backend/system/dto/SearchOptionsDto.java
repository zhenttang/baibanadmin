package com.yunke.backend.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchOptionsDto {
    private List<String> fields;
    private List<SearchHighlightDto> highlights;
    private SearchPaginationDto pagination;
}