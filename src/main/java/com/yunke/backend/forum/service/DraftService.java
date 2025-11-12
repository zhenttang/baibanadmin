package com.yunke.backend.forum.service;

import com.yunke.backend.forum.dto.CreateDraftRequest;
import com.yunke.backend.forum.dto.DraftDTO;
import com.yunke.backend.forum.dto.CreatePostRequest;
import com.yunke.backend.forum.dto.PostDTO;
import com.yunke.backend.system.domain.entity.PostDraft;
import com.yunke.backend.system.repository.PostDraftRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DraftService {

    private final PostDraftRepository postDraftRepository;
    private final PostService postService;

    @Transactional(rollbackFor = Exception.class)
    public DraftDTO saveDraft(CreateDraftRequest request) {
        String currentUserId = getCurrentUserId();
        return saveDraft(request, currentUserId);
    }

    @Transactional(rollbackFor = Exception.class)
    public DraftDTO saveDraft(CreateDraftRequest request, String currentUserId) {
        if (currentUserId == null || currentUserId.isBlank()) {
            throw new IllegalArgumentException("未登录或无法获取用户信息");
        }
        if (request == null) {
            throw new IllegalArgumentException("请求不能为空");
        }
        if (request.getForumId() == null) {
            throw new IllegalArgumentException("forumId不能为空");
        }

        PostDraft draft;
        if (request.getId() != null) {
            draft = postDraftRepository.findById(request.getId())
                    .orElseThrow(() -> new EntityNotFoundException("草稿不存在"));
            if (!currentUserId.equals(draft.getUserId())) {
                throw new IllegalArgumentException("无权限操作他人草稿");
            }
        } else {
            draft = new PostDraft();
            draft.setUserId(currentUserId);
        }

        draft.setForumId(request.getForumId());
        draft.setTitle(request.getTitle());
        draft.setContent(request.getContent());

        PostDraft saved = postDraftRepository.save(draft);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public DraftDTO getDraft(Long id, String currentUserId) {
        PostDraft draft = postDraftRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("草稿不存在"));
        if (!draft.getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("无权限查看他人草稿");
        }
        return toDTO(draft);
    }

    @Transactional(readOnly = true)
    public Page<DraftDTO> listMyDrafts(String userId, int page, int size) {
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        Pageable pageable = PageRequest.of(page, Math.min(size, 100), Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<PostDraft> p = postDraftRepository.findByUserId(userId, pageable);
        List<DraftDTO> dtos = p.getContent().stream().map(this::toDTO).collect(Collectors.toList());
        return new PageImpl<>(dtos, pageable, p.getTotalElements());
    }

    @Transactional(rollbackFor = Exception.class)
    public Boolean deleteDraft(Long id, String currentUserId) {
        PostDraft draft = postDraftRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("草稿不存在"));
        if (!draft.getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("无权限删除他人草稿");
        }
        postDraftRepository.deleteById(id);
        return true;
    }

    @Transactional(rollbackFor = Exception.class)
    public PostDTO publishDraft(Long id, String currentUserId) {
        PostDraft draft = postDraftRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("草稿不存在"));
        if (!draft.getUserId().equals(currentUserId)) {
            throw new IllegalArgumentException("无权限发布他人草稿");
        }

        CreatePostRequest req = new CreatePostRequest();
        req.setForumId(draft.getForumId());
        req.setTitle(draft.getTitle());
        req.setContent(draft.getContent());

        PostDTO post = postService.createPost(req);

        postDraftRepository.deleteById(id);
        return post;
    }

    private DraftDTO toDTO(PostDraft draft) {
        if (draft == null) return null;
        DraftDTO dto = new DraftDTO();
        dto.setId(draft.getId());
        dto.setUserId(draft.getUserId());
        dto.setForumId(draft.getForumId());
        dto.setTitle(draft.getTitle());
        dto.setContent(draft.getContent());
        dto.setCreatedAt(draft.getCreatedAt());
        dto.setUpdatedAt(draft.getUpdatedAt());
        return dto;
    }

    private String getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) return null;
        Object principal = authentication.getPrincipal();
        if (principal instanceof com.yunke.backend.security.AffineUserDetails aud) {
            return aud.getUserId();
        }
        return null;
    }
}
