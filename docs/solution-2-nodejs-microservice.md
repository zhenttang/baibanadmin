# 方案2: Node.js YJS微服务 + Java主服务（推荐⭐⭐⭐⭐⭐）

## 架构设计

```
┌─────────────────────────────────────────────────────────┐
│                    整体架构                                │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  前端 (React + YJS)                                      │
│    │                                                    │
│    ├─> WebSocket → Java Backend (Socket.IO 9092)       │
│    │                  │                                │
│    │                  ├─> 推送Update (原始二进制)         │
│    │                  │                                │
│    │                  ▼                                │
│    │            YJS微服务 (Node.js :3001)                │
│    │                  │                                │
│    │                  ├─> mergeUpdates()   ✅           │
│    │                  ├─> diffUpdate()     ✅           │
│    │                  ├─> encodeState()    ✅           │
│    │                  │                                │
│    │                  └─> 返回处理结果                   │
│    │                        │                          │
│    │                        ▼                          │
│    │            Java Backend                           │
│    │                  │                                │
│    │                  ├─> 保存到MySQL                   │
│    │                  ├─> 业务逻辑                       │
│    │                  ├─> 权限控制                       │
│    │                  └─> 其他服务                       │
│    │                                                    │
└─────────────────────────────────────────────────────────┘
```

## 技术栈

### Node.js微服务
- **框架**: Express.js / Fastify
- **YJS库**: 官方yjs包（原生TypeScript）
- **通信**: HTTP/gRPC/消息队列
- **端口**: 3001

### Java主服务
- **框架**: Spring Boot
- **通信客户端**: RestTemplate / WebClient / gRPC client
- **端口**: 8080

## Node.js微服务实现

### 项目结构
```
yjs-service/
├── package.json
├── src/
│   ├── index.js              // 服务入口
│   ├── yjs-handler.js        // YJS核心处理
│   ├── api/
│   │   ├── merge.js          // 合并接口
│   │   ├── diff.js           // 差异接口
│   │   └── encode.js         // 编码接口
│   └── utils/
│       └── logger.js
└── Dockerfile
```

### package.json
```json
{
  "name": "affine-yjs-service",
  "version": "1.0.0",
  "description": "YJS CRDT微服务，为 Yunke Java 后端提供 YJS 处理能力",
  "main": "src/index.js",
  "scripts": {
    "start": "node src/index.js",
    "dev": "nodemon src/index.js"
  },
  "dependencies": {
    "express": "^4.18.2",
    "yjs": "^13.6.10",
    "lib0": "^0.2.89",
    "cors": "^2.8.5",
    "body-parser": "^1.20.2"
  },
  "devDependencies": {
    "nodemon": "^3.0.2"
  }
}
```

### src/yjs-handler.js (核心处理逻辑)
```javascript
const Y = require('yjs');
const { encodeStateAsUpdate, encodeStateVector, applyUpdate, diffUpdate } = require('yjs');

class YjsHandler {
  /**
   * 合并多个YJS更新
   * @param {Array<Uint8Array>} updates - Base64编码的更新数组
   * @returns {Uint8Array} 合并后的更新
   */
  mergeUpdates(updates) {
    console.log(`🔄 [YjsHandler] 合并${updates.length}个更新`);

    const doc = new Y.Doc();

    // 应用所有更新
    updates.forEach((update, index) => {
      try {
        const buffer = this.base64ToUint8Array(update);
        Y.applyUpdate(doc, buffer);
        console.log(`  ✅ 应用更新 ${index + 1}/${updates.length}`);
      } catch (error) {
        console.error(`  ❌ 应用更新 ${index + 1} 失败:`, error.message);
      }
    });

    // 生成合并后的状态
    const merged = Y.encodeStateAsUpdate(doc);
    console.log(`✅ [YjsHandler] 合并完成: ${merged.length}字节`);

    return merged;
  }

  /**
   * 计算差异更新
   * @param {Uint8Array} update - 完整更新
   * @param {Uint8Array} stateVector - 客户端状态向量
   * @returns {Uint8Array} 差异更新
   */
  diffUpdate(update, stateVector) {
    const updateBuffer = this.base64ToUint8Array(update);
    const stateBuffer = stateVector ? this.base64ToUint8Array(stateVector) : null;

    if (!stateBuffer) {
      return updateBuffer; // 客户端状态为空，返回完整更新
    }

    const diff = Y.diffUpdate(updateBuffer, stateBuffer);
    console.log(`🔍 [YjsHandler] 差异计算: ${diff.length}字节`);

    return diff;
  }

  /**
   * 提取状态向量
   * @param {Uint8Array} update - 更新数据
   * @returns {Uint8Array} 状态向量
   */
  encodeStateVector(update) {
    const doc = new Y.Doc();
    const buffer = this.base64ToUint8Array(update);
    Y.applyUpdate(doc, buffer);

    const stateVector = Y.encodeStateVector(doc);
    console.log(`📊 [YjsHandler] 状态向量: ${stateVector.length}字节`);

    return stateVector;
  }

  /**
   * Base64 → Uint8Array
   */
  base64ToUint8Array(base64) {
    if (base64 instanceof Uint8Array) return base64;
    const binary = Buffer.from(base64, 'base64');
    return new Uint8Array(binary);
  }

  /**
   * Uint8Array → Base64
   */
  uint8ArrayToBase64(uint8Array) {
    return Buffer.from(uint8Array).toString('base64');
  }
}

module.exports = YjsHandler;
```

### src/index.js (HTTP服务)
```javascript
const express = require('express');
const bodyParser = require('body-parser');
const cors = require('cors');
const YjsHandler = require('./yjs-handler');

const app = express();
const PORT = process.env.PORT || 3001;

app.use(cors());
app.use(bodyParser.json({ limit: '50mb' }));

const yjsHandler = new YjsHandler();

// 健康检查
app.get('/health', (req, res) => {
  res.json({ status: 'ok', service: 'yjs-service' });
});

// 合并更新接口
app.post('/api/yjs/merge', (req, res) => {
  try {
    const { updates } = req.body;

    if (!Array.isArray(updates) || updates.length === 0) {
      return res.status(400).json({ error: '无效的updates数组' });
    }

    const merged = yjsHandler.mergeUpdates(updates);
    const base64 = yjsHandler.uint8ArrayToBase64(merged);

    res.json({
      success: true,
      merged: base64,
      size: merged.length
    });
  } catch (error) {
    console.error('❌ [YjsService] 合并失败:', error);
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

// 差异计算接口
app.post('/api/yjs/diff', (req, res) => {
  try {
    const { update, stateVector } = req.body;

    const diff = yjsHandler.diffUpdate(update, stateVector);
    const base64 = yjsHandler.uint8ArrayToBase64(diff);

    res.json({
      success: true,
      diff: base64,
      size: diff.length
    });
  } catch (error) {
    console.error('❌ [YjsService] 差异计算失败:', error);
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

// 状态向量提取接口
app.post('/api/yjs/state-vector', (req, res) => {
  try {
    const { update } = req.body;

    const stateVector = yjsHandler.encodeStateVector(update);
    const base64 = yjsHandler.uint8ArrayToBase64(stateVector);

    res.json({
      success: true,
      stateVector: base64,
      size: stateVector.length
    });
  } catch (error) {
    console.error('❌ [YjsService] 状态向量提取失败:', error);
    res.status(500).json({
      success: false,
      error: error.message
    });
  }
});

app.listen(PORT, () => {
  console.log(`🚀 YJS微服务已启动: http://localhost:${PORT}`);
  console.log(`📊 健康检查: http://localhost:${PORT}/health`);
});
```

### Dockerfile
```dockerfile
FROM node:18-alpine

WORKDIR /app

COPY package*.json ./
RUN npm ci --only=production

COPY src ./src

EXPOSE 3001

CMD ["node", "src/index.js"]
```

## Java端集成

### 1. 创建YJS微服务客户端

```java
package com.affine.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class YjsServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(YjsServiceClient.class);

    @Value("${yjs.service.url:http://localhost:3001}")
    private String yjsServiceUrl;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public YjsServiceClient(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 调用Node.js微服务合并YJS更新
     */
    public byte[] mergeUpdates(List<byte[]> updates) {
        logger.info("🔄 调用YJS微服务合并{}个更新", updates.size());

        try {
            // 转换为Base64
            List<String> base64Updates = updates.stream()
                .map(update -> Base64.getEncoder().encodeToString(update))
                .toList();

            Map<String, Object> request = new HashMap<>();
            request.put("updates", base64Updates);

            // 调用微服务
            String url = yjsServiceUrl + "/api/yjs/merge";
            Map<String, Object> response = restTemplate.postForObject(
                url,
                request,
                Map.class
            );

            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                String mergedBase64 = (String) response.get("merged");
                byte[] merged = Base64.getDecoder().decode(mergedBase64);

                logger.info("✅ YJS微服务合并成功: {}字节", merged.length);
                return merged;
            } else {
                String error = response != null ? (String) response.get("error") : "unknown";
                throw new RuntimeException("YJS微服务合并失败: " + error);
            }

        } catch (Exception e) {
            logger.error("❌ 调用YJS微服务失败", e);
            throw new RuntimeException("YJS微服务调用失败", e);
        }
    }

    /**
     * 计算差异更新
     */
    public byte[] diffUpdate(byte[] update, byte[] stateVector) {
        logger.debug("🔍 调用YJS微服务计算差异");

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("update", Base64.getEncoder().encodeToString(update));
            if (stateVector != null) {
                request.put("stateVector", Base64.getEncoder().encodeToString(stateVector));
            }

            String url = yjsServiceUrl + "/api/yjs/diff";
            Map<String, Object> response = restTemplate.postForObject(
                url,
                request,
                Map.class
            );

            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                String diffBase64 = (String) response.get("diff");
                return Base64.getDecoder().decode(diffBase64);
            } else {
                throw new RuntimeException("YJS差异计算失败");
            }

        } catch (Exception e) {
            logger.error("❌ YJS差异计算失败", e);
            return update; // 失败时返回完整更新
        }
    }

    /**
     * 提取状态向量
     */
    public byte[] encodeStateVector(byte[] update) {
        logger.debug("📊 调用YJS微服务提取状态向量");

        try {
            Map<String, Object> request = new HashMap<>();
            request.put("update", Base64.getEncoder().encodeToString(update));

            String url = yjsServiceUrl + "/api/yjs/state-vector";
            Map<String, Object> response = restTemplate.postForObject(
                url,
                request,
                Map.class
            );

            if (response != null && Boolean.TRUE.equals(response.get("success"))) {
                String stateVectorBase64 = (String) response.get("stateVector");
                return Base64.getDecoder().decode(stateVectorBase64);
            } else {
                throw new RuntimeException("状态向量提取失败");
            }

        } catch (Exception e) {
            logger.error("❌ 状态向量提取失败", e);
            return new byte[0];
        }
    }
}
```

### 2. 替换YjsCrdtEngine

```java
// 在 DocStorageAdapter.java 中
@Autowired
private YjsServiceClient yjsServiceClient; // 替代 YjsCrdtEngine

// 修改squashUpdates方法
private DocUpdate squashUpdates(List<DocUpdate> updates) {
    if (updates.isEmpty()) {
        throw new IllegalArgumentException("没有更新可以合并");
    }

    if (updates.size() == 1) {
        return updates.get(0);
    }

    // 提取所有更新的二进制数据
    List<byte[]> blobs = updates.stream()
        .map(DocUpdate::getBlob)
        .toList();

    // 🔥 调用Node.js微服务合并
    byte[] mergedBlob = yjsServiceClient.mergeUpdates(blobs);

    // 使用最后一个更新的时间戳和编辑者
    DocUpdate lastUpdate = updates.get(updates.size() - 1);

    return new DocUpdate(mergedBlob, lastUpdate.getTimestamp(), lastUpdate.getEditor());
}
```

### 3. 配置文件

```yaml
# application.yml
yjs:
  service:
    url: http://localhost:3001
    timeout: 5000
    retry: 3
```

## 部署方案

### 方案A: Docker Compose（开发/测试）
```yaml
version: '3.8'

services:
  # Node.js YJS微服务
  yjs-service:
    build: ./yjs-service
    ports:
      - "3001:3001"
    environment:
      - NODE_ENV=production
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:3001/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  # Java主服务
  affine-backend:
    build: .
    ports:
      - "8080:8080"
      - "9092:9092"
    environment:
      - YJS_SERVICE_URL=http://yjs-service:3001
    depends_on:
      - yjs-service
      - mysql
      - redis

  mysql:
    image: mysql:8.0
    # ... MySQL配置

  redis:
    image: redis:7-alpine
    # ... Redis配置
```

### 方案B: Kubernetes（生产）
```yaml
# yjs-service-deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: yjs-service
spec:
  replicas: 3
  selector:
    matchLabels:
      app: yjs-service
  template:
    metadata:
      labels:
        app: yjs-service
    spec:
      containers:
      - name: yjs-service
        image: affine/yjs-service:latest
        ports:
        - containerPort: 3001
        resources:
          requests:
            memory: "256Mi"
            cpu: "200m"
          limits:
            memory: "512Mi"
            cpu: "500m"
---
apiVersion: v1
kind: Service
metadata:
  name: yjs-service
spec:
  selector:
    app: yjs-service
  ports:
  - port: 3001
    targetPort: 3001
```

## 性能优化

### 1. 连接池配置
```java
@Configuration
public class YjsServiceConfig {

    @Bean
    public RestTemplate yjsRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);
        factory.setReadTimeout(5000);

        RestTemplate restTemplate = new RestTemplate(factory);
        return restTemplate;
    }
}
```

### 2. 批量处理
```javascript
// Node.js端支持批量操作
app.post('/api/yjs/batch-merge', async (req, res) => {
  const { batches } = req.body; // [{ docId, updates }, ...]

  const results = await Promise.all(
    batches.map(async ({ docId, updates }) => {
      const merged = yjsHandler.mergeUpdates(updates);
      return {
        docId,
        merged: yjsHandler.uint8ArrayToBase64(merged)
      };
    })
  );

  res.json({ success: true, results });
});
```

### 3. 缓存策略
```java
@Cacheable(value = "yjs-merged", key = "#updates.hashCode()")
public byte[] mergeUpdates(List<byte[]> updates) {
    return yjsServiceClient.mergeUpdates(updates);
}
```

## 实施计划

### 第1天: Node.js微服务开发
- [x] 创建项目结构
- [ ] 实现核心YJS处理
- [ ] HTTP接口开发
- [ ] 单元测试

### 第2天: Java集成
- [ ] YjsServiceClient实现
- [ ] 替换YjsCrdtEngine
- [ ] 配置管理
- [ ] 集成测试

### 第3天: 部署与优化
- [ ] Docker镜像构建
- [ ] Docker Compose配置
- [ ] 性能测试
- [ ] 监控配置

## 优势
✅ **开发快速**: 3-5天完成
✅ **使用官方YJS**: 100%兼容性
✅ **职责清晰**: Node.js只做CRDT，Java做业务
✅ **易于维护**: YJS升级只需更新npm包
✅ **可扩展**: 微服务独立扩容
✅ **技术成熟**: Express + YJS都很稳定

## 劣势
❌ 增加部署复杂度（需要Node.js环境）
❌ 网络开销（进程间通信）
❌ 需要维护两个服务

## 监控指标
- YJS微服务响应时间
- 合并操作成功率
- 网络超时率
- 内存使用情况

## 建议
**强烈推荐此方案**，因为：
1. 快速实现（3-5天）
2. 使用官方YJS库（零风险）
3. 架构清晰（各司其职）
4. 易于扩展和维护
