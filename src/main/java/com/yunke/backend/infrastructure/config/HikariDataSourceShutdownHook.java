package com.yunke.backend.infrastructure.config;

import com.zaxxer.hikari.HikariDataSource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Component;

/**
 * HikariCP数据源关闭钩子
 * 防止应用关闭时的内存泄漏
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class HikariDataSourceShutdownHook implements DisposableBean {

    private final HikariDataSource dataSource;

    @Override
    public void destroy() {
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("Shutting down HikariCP DataSource...");
            try {
                dataSource.close();
                log.info("HikariCP DataSource shut down successfully");
            } catch (Exception e) {
                log.error("Error shutting down HikariCP DataSource", e);
            }
        }
    }
}