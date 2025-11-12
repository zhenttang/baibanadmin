package com.yunke.backend.document.collaboration.model;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 文档状态模型
 * 维护文档的当前状态和元信息
 */
@Data
@NoArgsConstructor
public class DocState {
    
    /**
     * 当前文档的二进制数据
     */
    private byte[] currentDoc;
    
    /**
     * 最后修改时间戳
     */
    private long lastModified;
    
    /**
     * 文档版本号
     */
    private long version;
    
    /**
     * 是否有待保存的更改
     */
    private boolean dirty;
    
    public DocState(byte[] currentDoc, long lastModified) {
        this.currentDoc = currentDoc;
        this.lastModified = lastModified;
        this.version = 1;
        this.dirty = false;
    }
    
    public DocState(byte[] currentDoc) {
        this(currentDoc, System.currentTimeMillis());
    }
    
    /**
     * 更新文档数据
     */
    public void setCurrentDoc(byte[] currentDoc) {
        this.currentDoc = currentDoc;
        this.lastModified = System.currentTimeMillis();
        this.version++;
        this.dirty = true;
    }
    
    /**
     * 标记为已保存
     */
    public void markClean() {
        this.dirty = false;
    }
    
    /**
     * 获取文档大小
     */
    public int getSize() {
        return currentDoc != null ? currentDoc.length : 0;
    }
    
    /**
     * 检查文档是否为空
     */
    public boolean isEmpty() {
        return currentDoc == null || currentDoc.length == 0;
    }
}