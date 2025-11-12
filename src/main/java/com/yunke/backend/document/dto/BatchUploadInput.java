package com.yunke.backend.document.dto;

import com.yunke.backend.storage.dto.SetBlobInput;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchUploadInput {
    private String key;
    private InputStream inputStream;
    private SetBlobInput metadata;
}