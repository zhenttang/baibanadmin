package com.yunke.backend.forum.service;

import com.yunke.backend.storage.dto.SetBlobInput;
import com.yunke.backend.forum.dto.AttachmentDTO;
import com.yunke.backend.forum.domain.entity.ForumPost;
import com.yunke.backend.system.domain.entity.PostAttachment;
import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.forum.repository.ForumPostRepository;
import com.yunke.backend.system.repository.PostAttachmentRepository;
import com.yunke.backend.user.repository.UserRepository;
import com.yunke.backend.storage.service.BlobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttachmentService {

    private static final long MAX_SIZE_BYTES = 50L * 1024 * 1024; // 50MB
    private static final String WORKSPACE = "forum";

    private final BlobService blobService;
    private final PostAttachmentRepository attachmentRepository;
    private final ForumPostRepository forumPostRepository;
    private final UserRepository userRepository;

    @Transactional(rollbackFor = Exception.class)
    public AttachmentDTO uploadAttachment(String postId, MultipartFile file, String currentUserId) {
        if (postId == null || postId.isBlank()) {
            throw new IllegalArgumentException("帖子ID不能为空");
        }
        if (currentUserId == null || currentUserId.isBlank()) {
            throw new IllegalArgumentException("未登录或无法获取用户信息");
        }

        validateFile(file);

        ForumPost post = forumPostRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));

        String originalFilename = sanitizeFilename(file.getOriginalFilename());
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("文件名不能为空");
        }

        String extension = extractExtension(originalFilename);
        String key = postId + "/" + originalFilename;

        String contentType = file.getContentType();
        Long size = file.getSize();

        Map<String, String> meta = new HashMap<>();
        meta.put("postId", postId);
        meta.put("uploaderId", currentUserId);
        meta.put("filename", originalFilename);

        SetBlobInput input = SetBlobInput.builder()
                .contentType(contentType)
                .filename(originalFilename)
                .contentLength(size)
                .metadata(meta)
                .overwrite(false)
                .build();

        try (InputStream is = file.getInputStream()) {
            // 上传到Blob存储（Reactive转同步）
            String storedKey = blobService.setBlob(WORKSPACE, key, is, input, currentUserId).block();
            if (storedKey == null) {
                throw new RuntimeException("上传失败: 存储键为空");
            }

            // 生成可访问URL（通过BlobController路由）
            String fileUrl = "/api/workspaces/" + WORKSPACE + "/blobs/" + storedKey;

            // 查询上传者名称
            String uploaderName = userRepository.findById(currentUserId)
                    .map(User::getName)
                    .orElse(null);

            // 保存附件记录
            PostAttachment entity = new PostAttachment();
            entity.setPostId(postId);
            entity.setFileUrl(fileUrl);
            entity.setFileName(originalFilename);
            entity.setFileType(extension);
            entity.setFileSize(size);
            entity.setUploaderId(currentUserId);
            entity.setUploaderName(uploaderName);

            PostAttachment saved = attachmentRepository.save(entity);
            return toDTO(saved);

        } catch (Exception e) {
            log.error("上传附件失败: postId={}, filename={}, err={}", postId, originalFilename, e.getMessage(), e);
            throw new RuntimeException("上传失败: " + e.getMessage(), e);
        }
    }

    @Transactional(readOnly = true)
    public List<AttachmentDTO> getPostAttachments(String postId) {
        if (postId == null || postId.isBlank()) {
            throw new IllegalArgumentException("帖子ID不能为空");
        }
        List<PostAttachment> list = attachmentRepository.findByPostIdOrderByCreatedAtDesc(postId);
        List<AttachmentDTO> result = new ArrayList<>(list.size());
        for (PostAttachment a : list) {
            result.add(toDTO(a));
        }
        return result;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean deleteAttachment(Long id, String currentUserId) {
        if (id == null) {
            throw new IllegalArgumentException("附件ID不能为空");
        }
        if (currentUserId == null || currentUserId.isBlank()) {
            throw new IllegalArgumentException("未登录或无法获取用户信息");
        }

        PostAttachment attachment = attachmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("附件不存在"));

        ForumPost post = forumPostRepository.findById(attachment.getPostId())
                .orElseThrow(() -> new IllegalArgumentException("帖子不存在"));

        // 权限：上传者或帖子作者可删除
        if (!currentUserId.equals(attachment.getUploaderId()) && !currentUserId.equals(post.getUserId())) {
            throw new IllegalStateException("无权删除该附件");
        }

        String key = extractKeyFromUrl(attachment.getFileUrl());
        if (key == null || key.isBlank()) {
            throw new IllegalStateException("无法解析附件存储键");
        }

        try {
            // 同步删除Blob
            Boolean deleted = blobService.deleteBlob(WORKSPACE, key, true, currentUserId).block();
            if (Boolean.FALSE.equals(deleted)) {
                log.warn("删除Blob返回false, 但继续删除记录. workspace={}, key={}", WORKSPACE, key);
            }
        } catch (Exception e) {
            // 记录错误但继续删除记录，避免数据残留
            log.error("删除Blob失败: workspace={}, key={}, err={}", WORKSPACE, key, e.getMessage(), e);
        }

        attachmentRepository.delete(attachment);
        return true;
    }

    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("文件不能为空");
        }
        if (file.getSize() > MAX_SIZE_BYTES) {
            throw new IllegalArgumentException("文件大小不能超过50MB");
        }

        String filename = file.getOriginalFilename();
        String ext = extractExtension(filename);
        if (!isAllowedExtension(ext)) {
            throw new IllegalArgumentException("不支持的文件类型: " + ext);
        }
    }

    private boolean isAllowedExtension(String ext) {
        if (ext == null) return false;
        String e = ext.toLowerCase();
        return e.equals("jpg") || e.equals("jpeg") || e.equals("png") || e.equals("gif") || e.equals("webp")
                || e.equals("pdf") || e.equals("doc") || e.equals("docx")
                || e.equals("zip") || e.equals("rar");
    }

    private String extractExtension(String filename) {
        if (filename == null) return null;
        int idx = filename.lastIndexOf('.');
        if (idx > -1 && idx < filename.length() - 1) {
            return filename.substring(idx + 1);
        }
        return "";
    }

    private String sanitizeFilename(String original) {
        if (original == null) return null;
        String name = original;
        int slash = Math.max(name.lastIndexOf('/'), name.lastIndexOf('\\'));
        if (slash >= 0) {
            name = name.substring(slash + 1);
        }
        // 简单清理潜在危险字符
        return name.replace("..", "").replace("\n", "").replace("\r", "");
    }

    private String extractKeyFromUrl(String fileUrl) {
        if (fileUrl == null) return null;
        String prefix = "/api/workspaces/" + WORKSPACE + "/blobs/";
        if (fileUrl.startsWith(prefix)) {
            return fileUrl.substring(prefix.length());
        }
        return null;
    }

    private AttachmentDTO toDTO(PostAttachment entity) {
        AttachmentDTO dto = new AttachmentDTO();
        dto.setId(entity.getId());
        dto.setPostId(entity.getPostId());
        dto.setFileUrl(entity.getFileUrl());
        dto.setFileName(entity.getFileName());
        dto.setFileType(entity.getFileType());
        dto.setFileSize(entity.getFileSize());
        dto.setUploaderId(entity.getUploaderId());
        dto.setUploaderName(entity.getUploaderName());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}

