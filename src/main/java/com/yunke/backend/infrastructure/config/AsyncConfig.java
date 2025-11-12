package com.yunke.backend.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 异步任务调度配置
 * 为AsyncDocMergeService提供TaskScheduler支持
 */
@Configuration
@EnableAsync
@EnableScheduling
public class AsyncConfig {
    
    /**
     * 文档合并任务调度器
     * 专门用于异步文档合并操作
     */
    @Bean("docMergeTaskScheduler")
    public TaskScheduler docMergeTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        
        // 核心线程数：支持并发合并多个文档
        scheduler.setPoolSize(10);
        
        // 线程名前缀
        scheduler.setThreadNamePrefix("DocMerge-");
        
        // 等待任务完成后关闭
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(60);
        
        // 拒绝策略：调用者运行（确保任务不丢失）
        scheduler.setRejectedExecutionHandler(new java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy());
        
        scheduler.initialize();
        return scheduler;
    }
    
    /**
     * 通用任务调度器（如果需要其他异步任务）
     */
    @Bean("generalTaskScheduler")
    public TaskScheduler generalTaskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        
        scheduler.setPoolSize(5);
        scheduler.setThreadNamePrefix("AsyncTask-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        
        scheduler.initialize();
        return scheduler;
    }
}