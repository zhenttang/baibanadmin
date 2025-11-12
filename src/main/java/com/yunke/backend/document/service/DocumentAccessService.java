package com.yunke.backend.document.service;

import com.yunke.backend.document.dto.response.DocumentAccessResult;
import com.yunke.backend.community.domain.entity.CommunityDocument;
import com.yunke.backend.community.repository.CommunityDocumentRepository;

import com.yunke.backend.document.repository.DocumentPurchaseRepository;
import com.yunke.backend.user.repository.UserFollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 文档访问权限服务
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentAccessService {

    private final DocumentPurchaseRepository purchaseRepository;
    private final UserFollowRepository followRepository;
    private final CommunityDocumentRepository documentRepository;

    /**
     * 检查用户是否可以访问文档完整内容
     */
    public DocumentAccessResult checkAccess(String documentId, String userId) {
        CommunityDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("文档不存在"));

        // 作者本人可以访问
        if (userId != null && document.getAuthorId().equals(userId)) {
            return DocumentAccessResult.fullAccess();
        }

        // 检查是否需要关注
        if (document.getRequireFollow() != null && document.getRequireFollow()) {
            if (userId == null) {
                return DocumentAccessResult.needFollow();
            }
            boolean isFollowing = followRepository.existsByFollowerIdAndFollowingId(
                userId, document.getAuthorId());
            if (!isFollowing) {
                return DocumentAccessResult.needFollow();
            }
        }

        // 检查是否需要购买
        if (document.getIsPaid() != null && document.getIsPaid()) {
            if (userId == null) {
                return DocumentAccessResult.needPurchase(
                    document.getPrice(),
                    document.getDiscountPrice(),
                    document.getFreePreviewLength() != null ? document.getFreePreviewLength() : 200
                );
            }

            boolean hasPurchased = purchaseRepository.existsByDocumentIdAndUserIdAndStatus(
                documentId, userId, "completed");

            if (!hasPurchased) {
                return DocumentAccessResult.needPurchase(
                    document.getPrice(),
                    document.getDiscountPrice(),
                    document.getFreePreviewLength() != null ? document.getFreePreviewLength() : 200
                );
            }
        }

        return DocumentAccessResult.fullAccess();
    }

    /**
     * 获取文档内容（根据权限截断）
     */
    public String getDocumentContent(String documentId, String userId) {
        DocumentAccessResult access = checkAccess(documentId, userId);
        CommunityDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("文档不存在"));

        if (access.isHasFullAccess()) {
            return document.getContentSnapshot();
        }

        // 返回预览内容
        String content = document.getContentSnapshot();
        int previewLength = access.getPreviewLength();

        if (content != null && content.length() > previewLength) {
            return content.substring(0, previewLength) + "\n\n--- 需要购买后查看完整内容 ---";
        }

        return content;
    }

    /**
     * 检查用户是否可以编辑文档
     */
    public boolean canEdit(String documentId, String userId) {
        if (userId == null) {
            return false;
        }

        CommunityDocument document = documentRepository.findById(documentId)
            .orElseThrow(() -> new RuntimeException("文档不存在"));

        return document.getAuthorId().equals(userId);
    }

    /**
     * 检查用户是否可以删除文档
     */
    public boolean canDelete(String documentId, String userId) {
        return canEdit(documentId, userId);
    }
}
