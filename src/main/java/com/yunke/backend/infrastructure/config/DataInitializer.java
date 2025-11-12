package com.yunke.backend.infrastructure.config;

import com.yunke.backend.user.domain.entity.User;
import com.yunke.backend.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 数据初始化器 - 创建测试用户
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        initializeTestUsers();
    }

    private void initializeTestUsers() {
        try {
            // 创建管理员用户（简单用户名作为邮箱）
            String adminEmail = "admin";
            
            if (!userRepository.existsByEmail(adminEmail)) {
                User adminUser = User.builder()
                        .id(UUID.randomUUID().toString())
                        .name("管理员")
                        .email(adminEmail)
                        .password(passwordEncoder.encode("admin"))
                        .emailVerifiedAt(LocalDateTime.now())
                        .registered(true)
                        .disabled(false)
                        .enabled(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                
                userRepository.save(adminUser);
                log.info("创建管理员用户: {} (密码: admin)", adminEmail);
            } else {
                log.info("管理员用户已存在: {}", adminEmail);
            }
            
            // 创建测试用户
            String testEmail = "test@example.com";
            
            if (!userRepository.existsByEmail(testEmail)) {
                User testUser = User.builder()
                        .id(UUID.randomUUID().toString())
                        .name("测试用户")
                        .email(testEmail)
                        .password(passwordEncoder.encode("password123"))
                        .emailVerifiedAt(LocalDateTime.now())
                        .registered(true)
                        .disabled(false)
                        .enabled(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                
                userRepository.save(testUser);
                log.info("创建测试用户: {} (密码: password123)", testEmail);
            } else {
                log.info("测试用户已存在: {}", testEmail);
            }
            
            // 创建更多测试用户
            createUserIfNotExists("admin@example.com", "管理员邮箱", "admin123");
            createUserIfNotExists("user@example.com", "普通用户", "user123");
        } catch (Exception e) {
            log.error("初始化测试用户失败: {}", e.getMessage(), e);
        }
    }
    
    private void createUserIfNotExists(String email, String name, String password) {
        try {
            if (!userRepository.existsByEmail(email)) {
                User user = User.builder()
                        .id(UUID.randomUUID().toString())
                        .name(name)
                        .email(email)
                        .password(passwordEncoder.encode(password))
                        .emailVerifiedAt(LocalDateTime.now())
                        .registered(true)
                        .disabled(false)
                        .enabled(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                
                userRepository.save(user);
                log.info("创建用户: {} (密码: {})", email, password);
            }
        } catch (Exception e) {
            log.error("创建用户失败 {}: {}", email, e.getMessage());
        }
    }
}