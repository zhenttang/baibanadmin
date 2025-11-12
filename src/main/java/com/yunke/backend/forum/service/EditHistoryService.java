package com.yunke.backend.forum.service;

import com.yunke.backend.forum.dto.EditHistoryDTO;
import com.yunke.backend.system.domain.entity.PostEditHistory;
import com.yunke.backend.system.repository.PostEditHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EditHistoryService {

    private final PostEditHistoryRepository postEditHistoryRepository;

    @Transactional(rollbackFor = Exception.class)
    public void recordEdit(String postId, String oldTitle, String oldContent, String editorId, String editorName) {
        if (postId == null || postId.isBlank()) {
            throw new IllegalArgumentException("postId不能为空");
        }
        PostEditHistory history = new PostEditHistory();
        history.setPostId(postId);
        history.setOldTitle(oldTitle);
        history.setOldContent(oldContent);
        history.setEditorId(editorId);
        history.setEditorName(editorName);
        postEditHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public Page<EditHistoryDTO> getPostHistory(String postId, int page, int size) {
        if (postId == null || postId.isBlank()) {
            throw new IllegalArgumentException("postId不能为空");
        }
        if (page < 0) page = 0;
        if (size <= 0) size = 20;
        Pageable pageable = PageRequest.of(page, Math.min(size, 100));
        Page<PostEditHistory> p = postEditHistoryRepository.findByPostIdOrderByEditedAtDesc(postId, pageable);
        List<EditHistoryDTO> list = p.getContent().stream().map(this::toDTO).collect(Collectors.toList());
        return new PageImpl<>(list, pageable, p.getTotalElements());
    }

    @Transactional(readOnly = true)
    public EditHistoryDTO getHistoryDetail(Long historyId) {
        if (historyId == null) {
            throw new IllegalArgumentException("historyId不能为空");
        }
        PostEditHistory entity = postEditHistoryRepository.findById(historyId)
                .orElseThrow(() -> new IllegalArgumentException("历史记录不存在"));
        return toDTO(entity);
    }

    private EditHistoryDTO toDTO(PostEditHistory e) {
        if (e == null) return null;
        EditHistoryDTO dto = new EditHistoryDTO();
        dto.setId(e.getId());
        dto.setPostId(e.getPostId());
        dto.setOldTitle(e.getOldTitle());
        dto.setOldContent(e.getOldContent());
        dto.setEditorId(e.getEditorId());
        dto.setEditorName(e.getEditorName());
        dto.setEditedAt(e.getEditedAt());
        return dto;
    }
}

