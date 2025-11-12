package com.yunke.backend.document.service.impl;

import com.yunke.backend.common.exception.ResourceNotFoundException;
import com.yunke.backend.document.dto.DocDefaultRoleDto;
import com.yunke.backend.document.dto.DocRoleGrantRequest;
import com.yunke.backend.document.dto.DocRolePageDto;
import com.yunke.backend.document.dto.DocRolePageDto.DocRoleEdgeDto;
import com.yunke.backend.document.dto.DocRolePageDto.DocRoleNodeDto;
import com.yunke.backend.document.dto.DocRolePageDto.DocRolePageInfoDto;
import com.yunke.backend.document.dto.DocRolePageDto.DocRoleUserDto;
import com.yunke.backend.document.dto.DocRoleUpdateRequest;
import com.yunke.backend.document.service.DocRoleMapper;
import com.yunke.backend.document.service.DocRoleService;
import com.yunke.backend.workspace.domain.entity.WorkspaceDoc;
import com.yunke.backend.workspace.domain.entity.WorkspaceDocUserRole;
import com.yunke.backend.workspace.repository.WorkspaceDocUserRoleRepository;
import com.yunke.backend.workspace.service.WorkspaceDocService;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class DocRoleServiceImpl implements DocRoleService {

    private static final short DEFAULT_ROLE_TYPE = 20;
    private static final int MAX_PAGE_SIZE = 50;

    private final WorkspaceDocUserRoleRepository workspaceDocUserRoleRepository;
    private final WorkspaceDocService workspaceDocService;
    private final DocRoleMapper docRoleMapper;

    public DocRoleServiceImpl(WorkspaceDocUserRoleRepository workspaceDocUserRoleRepository,
                              WorkspaceDocService workspaceDocService,
                              DocRoleMapper docRoleMapper) {
        this.workspaceDocUserRoleRepository = workspaceDocUserRoleRepository;
        this.workspaceDocService = workspaceDocService;
        this.docRoleMapper = docRoleMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public DocRolePageDto listDocRoles(String workspaceId, String docId, int first, String after) {
        List<WorkspaceDocUserRole> allRoles = workspaceDocUserRoleRepository.findByWorkspaceIdAndDocId(workspaceId, docId);
        allRoles.sort(Comparator.comparing(WorkspaceDocUserRole::getUserId));

        int pageSize = Math.min(Math.max(first, 1), MAX_PAGE_SIZE);
        int startIndex = resolveStartIndex(after, allRoles);
        int endIndex = Math.min(startIndex + pageSize, allRoles.size());
        List<WorkspaceDocUserRole> page = allRoles.subList(startIndex, endIndex);

        List<DocRoleEdgeDto> edges = new ArrayList<>(page.size());
        for (WorkspaceDocUserRole role : page) {
            int mask = Optional.ofNullable(role.getPermissionMask()).orElse(0);
            DocRoleUserDto userDto = new DocRoleUserDto(
                    role.getUserId(),
                    role.getUser() != null ? role.getUser().getName() : null,
                    role.getUser() != null ? role.getUser().getAvatarUrl() : null
            );
            DocRoleNodeDto nodeDto = new DocRoleNodeDto(
                    userDto,
                    docRoleMapper.maskToRole(mask),
                    mask,
                    role.getType()
            );
            edges.add(new DocRoleEdgeDto(nodeDto));
        }

        boolean hasNextPage = endIndex < allRoles.size();
        String endCursor = page.isEmpty() ? null : page.get(page.size() - 1).getUserId();
        DocRolePageInfoDto pageInfo = new DocRolePageInfoDto(hasNextPage, endCursor);
        return new DocRolePageDto(edges, pageInfo, allRoles.size());
    }

    @Override
    public void grantDocRoles(String workspaceId, String docId, DocRoleGrantRequest request) {
        List<String> targetUserIds = request.resolvedUserIds();
        if (targetUserIds.isEmpty()) {
            throw new IllegalArgumentException("userIds or userId must be provided");
        }
        int mask = docRoleMapper.resolveMask(request.role(), request.permissionMask());
        short type = request.type() != null ? request.type() : DEFAULT_ROLE_TYPE;
        for (String userId : targetUserIds) {
            WorkspaceDocUserRole role = workspaceDocUserRoleRepository
                    .findByWorkspaceIdAndDocIdAndUserId(workspaceId, docId, userId)
                    .orElseGet(() -> WorkspaceDocUserRole.builder()
                            .workspaceId(workspaceId)
                            .docId(docId)
                            .userId(userId)
                            .build());
            role.setPermissionMask(mask);
            role.setType(type);
            workspaceDocUserRoleRepository.save(role);
        }
    }

    @Override
    public void updateDocRole(String workspaceId, String docId, String userId, DocRoleUpdateRequest request) {
        WorkspaceDocUserRole role = workspaceDocUserRoleRepository
                .findByWorkspaceIdAndDocIdAndUserId(workspaceId, docId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Doc role not found"));
        int mask = docRoleMapper.resolveMask(request.role(), request.permissionMask());
        role.setPermissionMask(mask);
        role.setType(resolveType(request.type()));
        workspaceDocUserRoleRepository.save(role);
    }

    @Override
    public void deleteDocRole(String workspaceId, String docId, String userId) {
        workspaceDocUserRoleRepository.deleteByWorkspaceIdAndDocIdAndUserId(workspaceId, docId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public DocDefaultRoleDto getDefaultRole(String workspaceId, String docId) {
        WorkspaceDoc doc = loadDoc(workspaceId, docId);
        int mask = doc.getDefaultPermissionMask() != null
                ? doc.getDefaultPermissionMask()
                : docRoleMapper.roleToMask(legacyRoleToName(doc.getDefaultRole()));
        return new DocDefaultRoleDto(docRoleMapper.maskToRole(mask), mask, Optional.ofNullable(doc.getDefaultRole()).orElse(0));
    }

    @Override
    public DocDefaultRoleDto updateDefaultRole(String workspaceId, String docId, DocRoleUpdateRequest request) {
        WorkspaceDoc doc = loadDoc(workspaceId, docId);
        int mask = docRoleMapper.resolveMask(request.role(), request.permissionMask());
        doc.setDefaultPermissionMask(mask);
        String roleName = request.role() != null ? request.role() : docRoleMapper.maskToRole(mask);
        doc.setDefaultRole(roleNameToLegacy(roleName));
        workspaceDocService.updateDoc(doc);
        return new DocDefaultRoleDto(roleName, mask, Optional.ofNullable(doc.getDefaultRole()).orElse(0));
    }

    private short resolveType(Short requestedType) {
        return requestedType != null ? requestedType : DEFAULT_ROLE_TYPE;
    }

    private int resolveStartIndex(String after, List<WorkspaceDocUserRole> roles) {
        if (after == null || after.isBlank()) {
            return 0;
        }
        for (int i = 0; i < roles.size(); i++) {
            if (after.equals(roles.get(i).getUserId())) {
                return i + 1;
            }
        }
        return 0;
    }

    private WorkspaceDoc loadDoc(String workspaceId, String docId) {
        WorkspaceDoc doc = workspaceDocService.findById(docId)
                .orElseThrow(() -> new ResourceNotFoundException("Document not found"));
        if (doc.getWorkspaceId() != null && !workspaceId.equals(doc.getWorkspaceId())) {
            throw new ResourceNotFoundException("Document not found");
        }
        return doc;
    }

    private String legacyRoleToName(Integer legacyRole) {
        if (legacyRole == null) {
            return "none";
        }
        return switch (legacyRole) {
            case 30 -> "manager";
            case 20 -> "editor";
            case 40 -> "reader";
            default -> "none";
        };
    }

    private int roleNameToLegacy(String role) {
        if (role == null) {
            return 0;
        }
        return switch (role) {
            case "manager" -> 30;
            case "editor" -> 20;
            case "reader" -> 40;
            default -> 0;
        };
    }
}
