package com.yunke.backend.document.crdt;

/**
 * YDoc观察者接口 - 监听文档变更事件
 * 
 * 完全对应YJS的Observer模式
 * 用于响应文档更新、同步等事件
 */
public interface YDocObserver {
    
    /**
     * 文档更新事件
     * @param transaction 导致更新的事务
     */
    void onUpdate(Transaction transaction);
    
    /**
     * 文档同步事件（可选实现）
     * @param update 同步的更新数据
     * @param origin 更新来源
     */
    default void onSync(byte[] update, String origin) {
        // 默认空实现
    }
    
    /**
     * 文档销毁事件（可选实现）
     */
    default void onDestroy() {
        // 默认空实现
    }
}