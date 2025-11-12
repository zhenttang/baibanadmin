package com.yunke.backend.infrastructure.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.sql.DataSource;
import java.util.Properties;

/**
 * 数据库配置
 * 对应 Node.js 版本的 Prisma 配置
 */
@org.springframework.context.annotation.Configuration
@EnableConfigurationProperties(AffineConfig.class)
@EnableJpaRepositories(basePackages = "com.yunke.backend")
@EnableJpaAuditing
@EnableTransactionManagement
@RequiredArgsConstructor
@Slf4j
public class DatabaseConfig {

    private final AffineConfig affineConfig;

    /**
     * 数据源配置
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        AffineConfig.DatabaseConfig dbConfig = affineConfig.getDatabase();
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(dbConfig.getUrl());
        config.setUsername(dbConfig.getUsername());
        config.setPassword(dbConfig.getPassword());
        config.setDriverClassName(dbConfig.getDriver());
        
        // 连接池配置
        config.setMaximumPoolSize(dbConfig.getMaxPoolSize());
        config.setMinimumIdle(dbConfig.getMinPoolSize());
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(600000); // 10 minutes
        config.setMaxLifetime(1800000); // 30 minutes
        config.setLeakDetectionThreshold(60000); // 1 minute
        
        // 防止内存泄漏的配置
        config.setRegisterMbeans(false);
        config.setAllowPoolSuspension(false);
        
        // 连接池优化
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        config.addDataSourceProperty("useLocalSessionState", "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");
        config.addDataSourceProperty("cacheResultSetMetadata", "true");
        config.addDataSourceProperty("cacheServerConfiguration", "true");
        config.addDataSourceProperty("elideSetAutoCommits", "true");
        config.addDataSourceProperty("maintainTimeStats", "false");
        
        // MySQL 特定配置
        config.addDataSourceProperty("useUnicode", "true");
        config.addDataSourceProperty("characterEncoding", "utf8");
        config.addDataSourceProperty("useSSL", "false");
        config.addDataSourceProperty("serverTimezone", "UTC");
        
        log.info("Configuring DataSource with URL: {}", dbConfig.getUrl());
        return new HikariDataSource(config);
    }

    /**
     * EntityManagerFactory 配置
     */
    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean factory = new LocalContainerEntityManagerFactoryBean();
        factory.setDataSource(dataSource);
        factory.setPackagesToScan("com.yunke.backend");
        factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        factory.setJpaProperties(hibernateProperties());
        
        return factory;
    }

    /**
     * 事务管理器配置
     */
    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactory);
        return transactionManager;
    }

    /**
     * Hibernate 属性配置
     */
    private Properties hibernateProperties() {
        AffineConfig.DatabaseConfig dbConfig = affineConfig.getDatabase();
        
        Properties properties = new Properties();
        
        // 基本配置
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.MySQLDialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "none");
        properties.setProperty("hibernate.show_sql", String.valueOf(dbConfig.isShowSql()));
        properties.setProperty("hibernate.format_sql", String.valueOf(dbConfig.isFormatSql()));
        
        // 性能优化配置
        properties.setProperty("hibernate.jdbc.batch_size", "50");
        properties.setProperty("hibernate.jdbc.fetch_size", "50");
        properties.setProperty("hibernate.order_inserts", "true");
        properties.setProperty("hibernate.order_updates", "true");
        properties.setProperty("hibernate.batch_versioned_data", "true");
        
        // 缓存配置 - 暂时禁用二级缓存避免依赖问题
        properties.setProperty("hibernate.cache.use_second_level_cache", "false");
        properties.setProperty("hibernate.cache.use_query_cache", "false");
        // properties.setProperty("hibernate.cache.region.factory_class", 
        //     "org.hibernate.cache.jcache.JCacheRegionFactory");
        
        // 统计和监控 - 简化配置
        properties.setProperty("hibernate.generate_statistics", "false");
        // properties.setProperty("hibernate.session.events.log.LOG_QUERIES_SLOWER_THAN_MS", "100");
        
        // 时区配置 - 对应 Prisma 的 Timestamptz
        properties.setProperty("hibernate.jdbc.time_zone", "UTC");
        properties.setProperty("hibernate.timezone.default_storage", "NORMALIZE");
        
        // JSON 支持 - 简化配置
        // properties.setProperty("hibernate.type.json_format_mapper", 
        //     "com.vladmihalcea.hibernate.type.util.ObjectMapperJsonSerializer");
        
        // 连接配置
        properties.setProperty("hibernate.connection.isolation", "2"); // READ_COMMITTED
        properties.setProperty("hibernate.connection.autocommit", "false");
        
        // 日志配置
        if (dbConfig.isShowSql()) {
            properties.setProperty("hibernate.type.descriptor.sql.BasicBinder", "TRACE");
            properties.setProperty("hibernate.type.descriptor.sql.BasicExtractor", "DEBUG");
        }
        
        return properties;
    }

    /**
     * 数据源关闭钩子，防止内存泄漏
     */
    @Bean
    public HikariDataSourceShutdownHook hikariDataSourceShutdownHook(DataSource dataSource) {
        return new HikariDataSourceShutdownHook((HikariDataSource) dataSource);
    }

    /**
     * 读写分离配置（可选）
     */
    @Bean
    @ConditionalOnProperty(name = "affine.database.read-only.enabled", havingValue = "true")
    public DataSource readOnlyDataSource() {
        // 读库配置
        HikariConfig config = new HikariConfig();
        // 配置读库连接信息
        return new HikariDataSource(config);
    }
}

