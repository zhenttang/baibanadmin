package com.yunke.backend.document.repository;

import com.yunke.backend.document.domain.entity.DocumentTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文档标签Repository接口
 */
@Repository
public interface DocumentTagRepository extends JpaRepository<DocumentTag, Integer> {

    /**
     * 根据名称查找标签
     */
    Optional<DocumentTag> findByName(String name);

    /**
     * 根据名称列表查找标签
     */
    List<DocumentTag> findByNameIn(List<String> names);

    /**
     * 根据slug检查是否存在
     */
    boolean existsBySlug(String slug);

    /**
     * 检查标签名称是否存在（排除指定ID）
     */
    @Query("SELECT COUNT(dt) > 0 FROM DocumentTag dt WHERE dt.name = :name AND (:id IS NULL OR dt.id != :id)")
    boolean existsByNameAndIdNot(@Param("name") String name, @Param("id") Integer id);

    /**
     * 按使用次数排序查找热门标签
     */
    @Query("SELECT dt FROM DocumentTag dt ORDER BY dt.useCount DESC")
    List<DocumentTag> findPopularTags();

    /**
     * 查找使用次数大于指定值的标签
     */
    List<DocumentTag> findByUseCountGreaterThanOrderByUseCountDesc(Integer minUsageCount);

    /**
     * 根据名称模糊搜索标签
     */
    List<DocumentTag> findByNameContainingIgnoreCaseOrderByUseCountDesc(String name);

    /**
     * 增加标签使用次数
     */
    @Query("UPDATE DocumentTag dt SET dt.useCount = dt.useCount + 1 WHERE dt.id = :id")
    void incrementUsageCount(@Param("id") Integer id);

    /**
     * 减少标签使用次数
     */
    @Query("UPDATE DocumentTag dt SET dt.useCount = dt.useCount - 1 WHERE dt.id = :id AND dt.useCount > 0")
    void decrementUsageCount(@Param("id") Integer id);

    /**
     * 统计标签总数
     */
    @Query("SELECT COUNT(dt) FROM DocumentTag dt")
    Long countTotalTags();

    /**
     * 查找指定数量的热门标签
     */
    @Query("SELECT dt FROM DocumentTag dt ORDER BY dt.useCount DESC LIMIT :limit")
    List<DocumentTag> findTopPopularTags(@Param("limit") int limit);
}
