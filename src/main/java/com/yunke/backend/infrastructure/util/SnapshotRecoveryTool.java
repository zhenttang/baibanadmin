package com.yunke.backend.infrastructure.util;

import com.yunke.backend.system.domain.entity.Snapshot;
import com.yunke.backend.system.domain.entity.Update;
import com.yunke.backend.system.repository.SnapshotRepository;
import com.yunke.backend.system.repository.UpdateRepository;
import com.yunke.backend.document.service.YjsServiceClient;
import com.yunke.backend.storage.binary.DocBinaryStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 快照恢复工具
 * 从updates表重建损坏的snapshots表数据
 *
 * 使用方法：
 * java -jar app.jar --spring.profiles.active=recovery
 */
@Slf4j
@Component
@Profile("recovery")  // 只在 recovery profile 下运行
@RequiredArgsConstructor
public class SnapshotRecoveryTool implements CommandLineRunner {

    private final SnapshotRepository snapshotRepository;
    private final UpdateRepository updateRepository;
    private final YjsServiceClient yjsServiceClient;
    private final DocBinaryStorageService binaryStorageService;

    @Override
    public void run(String... args) throws Exception {
        log.info("========================================");
        log.info("🔧 快照恢复工具启动");
        log.info("========================================");

        // 获取所有快照
        List<Snapshot> allSnapshots = snapshotRepository.findAll();
        log.info("📊 找到 {} 个快照记录", allSnapshots.size());

        int successCount = 0;
        int failCount = 0;
        int skipCount = 0;

        for (Snapshot snapshot : allSnapshots) {
            String workspaceId = snapshot.getWorkspaceId();
            String docId = snapshot.getId();

            log.info("-------------------------------------------");
            log.info("📄 处理文档: workspaceId={}, docId={}", workspaceId, docId);

            try {
                // 获取该文档的所有更新，按时间排序
                List<Update> updates = updateRepository
                        .findByWorkspaceIdAndId(workspaceId, docId)
                        .stream()
                        .sorted((u1, u2) -> u1.getCreatedAt().compareTo(u2.getCreatedAt()))
                        .collect(Collectors.toList());

                if (updates.isEmpty()) {
                    log.info("⚠️ 没有找到updates记录，跳过");
                    skipCount++;
                    continue;
                }

                log.info("📦 找到 {} 个更新记录", updates.size());

                // 提取所有更新的二进制数据
                List<byte[]> updateBlobs = updates.stream()
                        .map(update -> binaryStorageService.resolvePointer(update.getBlob()))
                        .collect(Collectors.toList());

                // 使用YJS微服务合并所有更新
                log.info("🔄 调用YJS微服务合并 {} 个更新...", updateBlobs.size());
                byte[] mergedBlob = yjsServiceClient.mergeUpdates(updateBlobs);

                log.info("✅ 合并成功: 大小={} 字节", mergedBlob.length);

                // 更新快照
                binaryStorageService.deletePointer(snapshot.getBlob());
                String pointer = binaryStorageService.saveSnapshot(workspaceId, docId, mergedBlob);
                snapshot.setBlob(binaryStorageService.pointerToBytes(pointer));
                snapshot.setUpdatedAt(LocalDateTime.now());
                snapshot.setSeq(snapshot.getSeq() + 1);

                snapshotRepository.save(snapshot);
                log.info("💾 快照已更新到数据库");

                successCount++;

            } catch (Exception e) {
                log.error("❌ 恢复失败: workspaceId={}, docId={}, error={}",
                        workspaceId, docId, e.getMessage(), e);
                failCount++;
            }
        }

        log.info("========================================");
        log.info("🎉 快照恢复完成");
        log.info("📊 统计: 成功={}, 失败={}, 跳过={}, 总计={}",
                successCount, failCount, skipCount, allSnapshots.size());
        log.info("========================================");

        // 退出程序
        System.exit(0);
    }
}

