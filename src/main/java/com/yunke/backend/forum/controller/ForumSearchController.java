package com.yunke.backend.controller.forum;

import com.yunke.backend.common.ApiResponse;
import com.yunke.backend.forum.dto.SearchRequest;
import com.yunke.backend.forum.dto.SearchResultDTO;
import com.yunke.backend.forum.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/forum/search")
@RequiredArgsConstructor
@Tag(name = "Forum Search")
public class ForumSearchController {
    
    private final SearchService searchService;
    
    @Operation(summary = "综合搜索")
    @PostMapping
    public ApiResponse<List<SearchResultDTO>> search(
            @Valid @RequestBody SearchRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(searchService.searchAll(request, page, size));
    }
    
    @Operation(summary = "快速搜索（GET方式）")
    @GetMapping
    public ApiResponse<List<SearchResultDTO>> quickSearch(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "ALL") String type,
            @RequestParam(required = false) Long forumId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        SearchRequest request = new SearchRequest();
        request.setKeyword(keyword);
        request.setType(type);
        request.setForumId(forumId);
        return ApiResponse.success(searchService.searchAll(request, page, size));
    }
}
