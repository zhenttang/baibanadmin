package com.yunke.backend.document.repository;

import com.yunke.backend.document.domain.entity.DocumentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 文档分类Repository接口
 */
@Repository
public interface DocumentCategoryRepository extends JpaRepository<DocumentCategory, Integer> {

    /**
     * 查找所有激活的分类，按排序序号排序
     */
    List<DocumentCategory> findByIsActiveTrueOrderBySortOrder();
    
    /**
     * 查找所有激活的分类
     */
    List<DocumentCategory> findByIsActiveTrue();

    /**
     * 根据名称查找分类
     */
    Optional<DocumentCategory> findByName(String name);
    
    /**
     * 检查分类名称是否存在
     */
    boolean existsByName(String name);

    /**
     * 检查分类名称是否存在（排除指定ID）
     */
    @Query("SELECT COUNT(dc) > 0 FROM DocumentCategory dc WHERE dc.name = :name AND (:id IS NULL OR dc.id != :id)")
    boolean existsByNameAndIdNot(@Param("name") String name, @Param("id") Integer id);

    /**
     * 查找指定排序序号范围内的分类
     */
    List<DocumentCategory> findBySortOrderBetweenOrderBySortOrder(Integer minOrder, Integer maxOrder);

    /**
     * 获取最大排序序号
     */
    @Query("SELECT COALESCE(MAX(dc.sortOrder), 0) FROM DocumentCategory dc")
    Integer findMaxSortOrder();

    /**
     * 统计激活的分类数量
     */
    @Query("SELECT COUNT(dc) FROM DocumentCategory dc WHERE dc.isActive = true")
    Long countActiveCategories();
}