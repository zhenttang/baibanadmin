package com.yunke.backend.search.provider;

import com.yunke.backend.system.dto.*;
import com.yunke.backend.search.enums.SearchProviderType;
import com.yunke.backend.search.enums.SearchTable;
import com.yunke.backend.search.dto.AggregateInputDto;
import com.yunke.backend.search.dto.AggregateResultDto;
import com.yunke.backend.search.dto.OperationOptionsDto;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;

/**
 * 搜索提供者抽象接口
 * 对应Node.js版本的SearchProvider抽象类
 */
public interface SearchProvider {

    /**
     * 获取搜索提供者类型
     */
    SearchProviderType getType();

    /**
     * 创建搜索表
     */
    Mono<Void> createTable(SearchTable table, String mapping);

    /**
     * 执行搜索查询
     */
    Mono<SearchResultDto> search(SearchTable table, SearchInputDto searchInput);

    /**
     * 执行聚合查询
     */
    Mono<AggregateResultDto> aggregate(SearchTable table, AggregateInputDto aggregateInput);

    /**
     * 写入文档到索引
     */
    Mono<Void> write(SearchTable table, List<Map<String, Object>> documents, OperationOptionsDto options);

    /**
     * 根据查询删除文档
     */
    Mono<Void> deleteByQuery(SearchTable table, Map<String, Object> query, OperationOptionsDto options);

    /**
     * 删除单个文档
     */
    Mono<Void> deleteDocument(SearchTable table, String documentId);

    /**
     * 检查表是否存在
     */
    Mono<Boolean> tableExists(SearchTable table);

    /**
     * 删除表
     */
    Mono<Void> deleteTable(SearchTable table);

    /**
     * 获取文档数量
     */
    Mono<Long> getDocumentCount(SearchTable table);

    /**
     * 刷新索引
     */
    Mono<Void> refresh(SearchTable table);

    /**
     * 健康检查
     */
    Mono<Boolean> isHealthy();
}

