package com.yunke.backend.document.repository;

import com.yunke.backend.document.domain.entity.DocumentPurchase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DocumentPurchaseRepository extends JpaRepository<DocumentPurchase, Long> {

    /**
     * 根据文档ID和用户ID查找购买记录
     */
    Optional<DocumentPurchase> findByDocumentIdAndUserId(String documentId, String userId);

    /**
     * 检查用户是否已购买文档
     */
    boolean existsByDocumentIdAndUserIdAndStatus(String documentId, String userId, String status);

    /**
     * 根据用户ID查找所有购买记录
     */
    Page<DocumentPurchase> findByUserIdAndStatus(String userId, String status, Pageable pageable);

    /**
     * 根据文档ID查找所有购买记录
     */
    Page<DocumentPurchase> findByDocumentIdAndStatus(String documentId, String status, Pageable pageable);

    /**
     * 根据支付订单号查找购买记录
     */
    Optional<DocumentPurchase> findByPaymentId(String paymentId);

    /**
     * 根据状态查找购买记录
     */
    Page<DocumentPurchase> findByStatus(String status, Pageable pageable);

    /**
     * 查找指定时间范围内的购买记录
     */
    @Query("SELECT dp FROM DocumentPurchase dp WHERE dp.purchasedAt BETWEEN :startDate AND :endDate AND dp.status = :status")
    List<DocumentPurchase> findByPurchasedAtBetweenAndStatus(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("status") String status
    );

    /**
     * 统计文档的购买数量
     */
    @Query("SELECT COUNT(dp) FROM DocumentPurchase dp WHERE dp.documentId = :documentId AND dp.status = 'completed'")
    Long countByDocumentId(@Param("documentId") String documentId);

    /**
     * 统计用户的购买数量
     */
    @Query("SELECT COUNT(dp) FROM DocumentPurchase dp WHERE dp.userId = :userId AND dp.status = 'completed'")
    Long countByUserId(@Param("userId") String userId);

    /**
     * 查找用户购买的所有文档ID
     */
    @Query("SELECT dp.documentId FROM DocumentPurchase dp WHERE dp.userId = :userId AND dp.status = 'completed'")
    List<String> findDocumentIdsByUserId(@Param("userId") String userId);

    /**
     * 查找购买指定文档的所有用户ID
     */
    @Query("SELECT dp.userId FROM DocumentPurchase dp WHERE dp.documentId = :documentId AND dp.status = 'completed'")
    List<String> findUserIdsByDocumentId(@Param("documentId") String documentId);

    /**
     * 统计指定时间范围内的销售总额
     */
    @Query("SELECT SUM(dp.price) FROM DocumentPurchase dp WHERE dp.purchasedAt BETWEEN :startDate AND :endDate AND dp.status = 'completed'")
    Long calculateTotalRevenue(
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );

    /**
     * 删除文档的所有购买记录（一般不应该删除）
     */
    void deleteByDocumentId(String documentId);
}
