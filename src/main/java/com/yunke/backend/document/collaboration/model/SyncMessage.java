package com.yunke.backend.document.collaboration.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 同步消息模型
 * 对应YJS协议的消息格式
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SyncMessage {
    
    /**
     * 消息类型
     * 0: YJS_MSG_SYNC
     * 1: YJS_MSG_AWARENESS
     * 2: YJS_MSG_AUTH
     * 3: YJS_MSG_QUERY_AWARENESS
     */
    private int messageType;
    
    /**
     * 同步类型 (仅当messageType=0时有效)
     * 0: YJS_SYNC_STEP1 - 请求状态向量
     * 1: YJS_SYNC_STEP2 - 发送更新
     * 2: YJS_SYNC_UPDATE - 增量更新
     */
    private int syncType;
    
    /**
     * 消息负载数据
     */
    private byte[] payload;
    
    public boolean isSyncMessage() {
        return messageType == 0;
    }
    
    public boolean isAwarenessMessage() {
        return messageType == 1;
    }
    
    public boolean isAuthMessage() {
        return messageType == 2;
    }
    
    public boolean isStep1() {
        return isSyncMessage() && syncType == 0;
    }
    
    public boolean isStep2() {
        return isSyncMessage() && syncType == 1;
    }
    
    public boolean isUpdate() {
        return isSyncMessage() && syncType == 2;
    }
}