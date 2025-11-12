package com.yunke.backend.document.service.impl;

import com.yunke.backend.document.dto.DocDefaultRoleDto;
import com.yunke.backend.document.dto.DocRoleUpdateRequest;
import com.yunke.backend.document.service.DocRoleMapper;
import com.yunke.backend.workspace.domain.entity.WorkspaceDoc;
import com.yunke.backend.workspace.repository.WorkspaceDocUserRoleRepository;
import com.yunke.backend.workspace.service.WorkspaceDocService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DocRoleServiceImplTest {

    @Mock
    private WorkspaceDocUserRoleRepository workspaceDocUserRoleRepository;

    @Mock
    private WorkspaceDocService workspaceDocService;

    private DocRoleServiceImpl docRoleService;

    @BeforeEach
    void setUp() {
        docRoleService = new DocRoleServiceImpl(
                workspaceDocUserRoleRepository,
                workspaceDocService,
                new DocRoleMapper()
        );
    }

    @Test
    void getDefaultRoleReturnsMaskFromStoredDoc() {
        WorkspaceDoc doc = WorkspaceDoc.builder()
                .workspaceId("ws1")
                .docId("doc1")
                .defaultPermissionMask(5)
                .defaultRole(20)
                .build();
        when(workspaceDocService.findById("doc1")).thenReturn(Optional.of(doc));

        DocDefaultRoleDto dto = docRoleService.getDefaultRole("ws1", "doc1");

        assertThat(dto.permissionMask()).isEqualTo(5);
        assertThat(dto.role()).isEqualTo("commenter");
    }

    @Test
    void updateDefaultRolePersistsDerivedLegacyValue() {
        WorkspaceDoc doc = WorkspaceDoc.builder()
                .workspaceId("ws1")
                .docId("doc1")
                .defaultRole(20)
                .build();
        when(workspaceDocService.findById("doc1")).thenReturn(Optional.of(doc));
        when(workspaceDocService.updateDoc(doc)).thenReturn(doc);

        DocRoleUpdateRequest request = new DocRoleUpdateRequest("manager", null, null);
        DocDefaultRoleDto dto = docRoleService.updateDefaultRole("ws1", "doc1", request);

        assertThat(dto.role()).isEqualTo("manager");
        assertThat(dto.legacyDefaultRole()).isEqualTo(30);
        assertThat(doc.getDefaultPermissionMask()).isNotNull();
        verify(workspaceDocService).updateDoc(doc);
    }
}
