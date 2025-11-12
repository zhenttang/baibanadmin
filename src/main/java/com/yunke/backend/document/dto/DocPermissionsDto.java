package com.yunke.backend.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocPermissionsDto {
    // Basic document permissions
    private Boolean docRead;
    private Boolean docCopy;
    private Boolean docDuplicate;
    private Boolean docTrash;
    private Boolean docRestore;
    private Boolean docDelete;
    private Boolean docUpdate;
    private Boolean docPublish;
    private Boolean docTransferOwner;
    
    // Document properties permissions
    private Boolean docPropertiesRead;
    private Boolean docPropertiesUpdate;
    
    // Document users permissions
    private Boolean docUsersRead;
    private Boolean docUsersManage;
}