package com.yunke.backend.document.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateTemplateInput {
    private String name;
    private String description;
    private String category;
    private String previewImage;
    private Boolean isPublic;
}