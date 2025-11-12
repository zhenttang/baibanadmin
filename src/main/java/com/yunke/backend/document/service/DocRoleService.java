package com.yunke.backend.document.service;

import com.yunke.backend.document.dto.DocDefaultRoleDto;
import com.yunke.backend.document.dto.DocRoleGrantRequest;
import com.yunke.backend.document.dto.DocRolePageDto;
import com.yunke.backend.document.dto.DocRoleUpdateRequest;

public interface DocRoleService {

    DocRolePageDto listDocRoles(String workspaceId, String docId, int first, String after);

    void grantDocRoles(String workspaceId, String docId, DocRoleGrantRequest request);

    void updateDocRole(String workspaceId, String docId, String userId, DocRoleUpdateRequest request);

    void deleteDocRole(String workspaceId, String docId, String userId);

    DocDefaultRoleDto getDefaultRole(String workspaceId, String docId);

    DocDefaultRoleDto updateDefaultRole(String workspaceId, String docId, DocRoleUpdateRequest request);
}
