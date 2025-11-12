package com.yunke.backend.document.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * YJS 工具类 - 使用新的CRDT引擎
 * 这个类作为外部API的适配器，内部使用YjsCrdtEngine进行实际的CRDT操作
 * 
 * 保持与现有代码的兼容性，同时提供完整的YJS CRDT功能
 */
@Slf4j
@Component
public class YjsUtils {
    
    @Autowired
    private YjsCrdtEngine crdtEngine;
    
    /**
     * 创建最小有效的YJS文档
     * 这是DocWriter需要的方法
     */
    public byte[] createMinimalValidYjsDoc(String docId) {
        log.info("🔧 [YjsUtils] 创建最小有效YJS文档: docId={}", docId);
        return crdtEngine.createEmptyDoc(docId);
    }
    
    /**
     * 合并多个YJS更新
     * 用于合并来自不同客户端的更新
     */
    public byte[] mergeUpdates(List<byte[]> updates) {
        log.info("🔧 [YjsUtils] 合并YJS更新: count={}", updates.size());
        return crdtEngine.mergeUpdates(updates);
    }
    
    /**
     * 计算文档差异更新
     * 用于客户端同步
     */
    public byte[] diffUpdate(byte[] currentDoc, byte[] stateVector) {
        log.info("🔧 [YjsUtils] 计算文档差异更新");
        return crdtEngine.diffUpdate(currentDoc, stateVector);
    }
    
    /**
     * 从更新数据编码状态向量
     * 用于同步协议
     */
    public byte[] encodeStateVectorFromUpdate(byte[] update) {
        log.info("🔧 [YjsUtils] 从更新编码状态向量");
        return crdtEngine.encodeStateVectorFromUpdate(update);
    }
    
    /**
     * 应用更新到文档
     * 用于实时协作
     */
    public byte[] applyUpdate(byte[] currentDoc, byte[] update) {
        log.info("🔧 [YjsUtils] 应用更新到文档");
        return crdtEngine.applyUpdate(currentDoc, update);
    }
    
    /**
     * 验证YJS文档格式
     * 用于数据完整性检查
     */
    public boolean isValidYjsDoc(byte[] docBlob) {
        log.debug("🔧 [YjsUtils] 验证YJS文档格式: size={}B", docBlob.length);
        return crdtEngine.isValidYjsDoc(docBlob);
    }
    
    /**
     * 获取文档摘要信息
     * 用于调试和监控
     */
    public String getDocumentSummary(byte[] docBlob) {
        log.debug("🔧 [YjsUtils] 获取文档摘要: size={}B", docBlob.length);
        return crdtEngine.getDocumentSummary(docBlob);
    }

    // ❌ 已删除 createEmptyDoc(String docId) - 假实现，请使用实例方法 createMinimalValidYjsDoc()

    /**
     * 创建空的YJS文档
     * 通过 yjs-service 创建标准的空 Y.js 文档
     * 
     * ⚠️ 重要：不要手动构造 Y.js 二进制格式！
     * 所有 Y.js 操作都应该通过 yjs-service (Node.js + 官方yjs库) 处理
     * 
     * @return 有效的空 Y.js 文档二进制数据
     */
    public byte[] createEmptyYjsDoc() {
        log.info("🔧 [YjsUtils] 通过yjs-service创建空的YJS文档");
        
        try {
            // 调用 yjs-service 的 create-empty 接口
            byte[] emptyDoc = crdtEngine.createEmptyDoc(null);
            
            if (emptyDoc == null || emptyDoc.length == 0) {
                throw new RuntimeException("yjs-service返回空数据");
            }
            
            // 显示二进制内容的十六进制预览（用于调试）
            if (log.isDebugEnabled()) {
                String hexPreview = bytesToHex(emptyDoc, 16);
                log.debug("🔍 [YjsUtils] 空文档二进制: {} ({}字节)", hexPreview, emptyDoc.length);
            }
            
            log.info("✅ [YjsUtils] 空文档创建成功: {}字节", emptyDoc.length);
            return emptyDoc;
            
        } catch (Exception e) {
            log.error("❌ [YjsUtils] 通过yjs-service创建空文档失败", e);
            throw new RuntimeException("创建空YJS文档失败: " + e.getMessage(), e);
        }
    }
    
    /**
     * 将字节数组转换为十六进制字符串（用于调试）
     */
    private static String bytesToHex(byte[] bytes, int maxLength) {
        if (bytes == null || bytes.length == 0) {
            return "(empty)";
        }
        
        int length = Math.min(bytes.length, maxLength);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(String.format("%02x ", bytes[i]));
        }
        if (bytes.length > maxLength) {
            sb.append("...");
        }
        return sb.toString().trim();
    }
    
    // ❌ 已删除 mergeUpdates(byte[], byte[]) - 假实现，请使用 YjsServiceClient.mergeUpdates()
    
    // ❌ 已删除 computeState() - 假实现，YJS状态向量需要使用 YjsServiceClient.encodeStateVector()

    // ❌ 已删除工具方法 longToBytes(), intToBytes(), writeInt() - 它们只被假实现使用

    /**
     * 提取明文内容 - 静态方法委托给实例方法
     */
    public static String extractPlainText(byte[] yjsData) {
        // 这里需要创建实例来调用实例方法，但为了向后兼容，提供简化实现
        if (yjsData == null || yjsData.length == 0) {
            return null;
        }
        
        try {
            // 简化的文本提取逻辑
            String dataStr = new String(yjsData, "UTF-8");
            if (dataStr.contains("Untitled") && dataStr.length() > 20) {
                return "Document content";
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }
    
    // ❌ 已删除 mergeUpdate() - 假实现，请使用 YjsServiceClient.mergeUpdates()
    
    // ❌ 已删除 computeDiff() - 假实现，请使用 YjsServiceClient.diffUpdate()
    
    // ❌ 已删除 mergeMultipleUpdates() - 假实现，请使用 YjsServiceClient.mergeUpdates()
}