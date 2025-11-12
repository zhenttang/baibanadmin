# 云科（YUNKE）Java Backend

> Spring Boot 3 / Java 21 implementation of the Yunke collaborative backend, refactored from the AFFiNE open backend. 本文档同时提供中文与英文信息，默认先展示中文，再附英文说明。

---

## 🇨🇳 中文说明

> 云科（YUNKE）Java 后端基于 Spring Boot 3 / Java 21，继承 AFFiNE 架构并针对云科业务深度重构，负责多租户工作区、CRDT 文档协作、支付、AI、通知、社区等核心能力。

```
浏览器 / 客户端 → REST & Socket.IO (8080 / 9092)
                        ↓
              Controller & DTO 层
                        ↓
             Service / Domain 层
          (workspace, document…)
                        ↓
        Repository 层 (JPA, Redis, Yjs)
                        ↓
MySQL 8  |  Redis 7  |  Yjs Node Service
```

### 核心能力
- **工作区 & 文档图谱**：支持组织→空间→文档层级，内建 RBAC/ACL，CRDT 状态保持一致。
- **CRDT 集成**：所有 merge/diff/state-vector 操作由 Node.js Yjs 微服务处理，避免主语言直接构造二进制。
- **实时与消息**：Socket.IO/Netty WebSocket 监听 `9092`，配合异步通知、邮件、社区/论坛模块。
- **支付抽象**：可插拔支付提供商（默认支付宝沙箱），支持回调、成功页、社区下单流程。
- **可观测性**：Actuator、Prometheus、结构化日志与依赖/角色检查脚本齐全。

### 项目结构
| 路径 | 说明 |
| --- | --- |
| `src/main/java/com/yunke/backend/` | 主代码，按域划分 `user`、`workspace`、`document`、`community`、`forum`、`notification`、`payment`、`ai`、`storage`、`system`、`security`、`infrastructure` 等包。|
| `src/main/resources/` | `application*.yml`、Flyway 迁移 (`db/migration`)、Logback、静态资源与模板。|
| `docs/` | 设计记录（如 `solution-2-nodejs-microservice.md` 描述 Yjs 微服务方案）。|
| `docker-compose.yml` | Java + Yjs + MySQL + Redis 的参考编排。|
| `prometheus.yml` | `/actuator/prometheus` 抓取示例。|
| `start.sh` / `test-role-system.sh` / `analyze_dependencies.sh` / `update_imports_batch.sh` | 常用脚本。|
| `storage/`、`uploads/`、`logs/` | 本地存储与日志挂载。|

### 模块导览
- `admin`：运维/管理端接口。
- `ai`：AI 助手、提示词、计费。
- `common`：DTO、常量、异常、工具。
- `community` / `forum`：社区内容、评论、购买。
- `document`：文档元数据、历史、与 Yjs 同步 API。
- `infrastructure`：配置、持久化、序列化等基础设施。
- `notification`：邮件 / SMS / Push 模块与模板。
- `payment`：支付抽象、Webhook、订单实体，默认接入支付宝沙箱。
- `search`：搜索适配层与索引策略。
- `security`：JWT、过滤器链、CORS、限流。
- `storage`：本地 / S3 / OSS 存储策略。
- `system`：特性开关、系统设置。
- `user`：账号、会话、组织成员。
- `workspace`：空间层级、邀请、角色、ACL。

### 配置
- Profile：`dev`（默认）、`test`、`prod`，通过 `SPRING_PROFILES_ACTIVE` 切换。
- 主配置：`application.yml` 定义 MySQL、Redis、Flyway、日志、Socket.IO/WebSocket、Yjs 地址等。
- 附加配置：`application-security.yml`、`application-performance.yml`、`application-server*.properties`；建议创建 `application.local.yml` 保存私密信息。
- 重点属性：`spring.datasource.*`、`spring.redis.*`、`yjs.service.url`、`affine.payment.*`、`affine.storage.*`、`socketio.server.*`、`websocket.*`。

### 外部依赖
| 服务 | 默认 | 备注 |
| --- | --- | --- |
| MySQL 8.0 | `localhost:3306` / 数据库 `affine` / 用户 `root` | Flyway 迁移位于 `classpath:db/migration`，完整 SQL dump 在上级 `../database/`。|
| Redis 7 | `localhost:6379` | 缓存、会话、Pub/Sub。|
| Yjs Node Service | `http://localhost:3001` | 负责所有 Yjs `create-empty`、`merge`、`diff`、`state-vector`、`batch`、`validate` 调用。|

### 本地运行
1. 准备 JDK 21、Maven 3.9+、MySQL 8、Redis 7、Node 18（用于 Yjs）。可导入 `../database/affine.sql`。
2. 配置：
   ```bash
   cd yunke-java-backend
   cp src/main/resources/application.yml src/main/resources/application.local.yml  # 可选
   ```
3. 构建：`mvn clean package -DskipTests`
4. 启动：`java -jar target/yunke-java-backend-0.21.0.jar --spring.profiles.active=dev`
   - REST：`http://localhost:8080`
   - Socket.IO/WebSocket：`ws://localhost:9092`
5. **Docker Compose**：`docker compose up -d` 同时拉起 `yjs-service`、`affine-backend`、`mysql`、`redis`，通过 `SPRING_PROFILES_ACTIVE`、`MYSQL_*`、`YJS_SERVICE_URL`、`REDIS_HOST` 等环境变量覆盖。

### 数据库 & 迁移
- Flyway 启动自动运行，脚本放入 `src/main/resources/db/migration`。
- 手工 SQL 工具在 `../database/`（基线、补丁、修复）。
- 修改 schema 时：编写迁移脚本 + 更新 SQL dump + 通知其它依赖方。

### 观测 & 运维
- Actuator：`/actuator/health`、`/actuator/info`、`/actuator/metrics`、`/actuator/prometheus`。
- Prometheus：参考 `prometheus.yml` 抓取 `http://<host>:8080/actuator/prometheus`。
- 日志：`logback-spring.xml` 输出至 `logs/affine-backend.log`。
- 健康检查：Docker/K8s 建议探测 `/actuator/health`。

### 脚本 & 工具
| 脚本 | 用途 |
| --- | --- |
| `start.sh` | 将 Jar 以守护进程运行。|
| `test-role-system.sh` | 校验角色/权限矩阵。|
| `analyze_dependencies.sh` | 输出 Maven 依赖树。|
| `update_imports_batch.sh` | 批量整理 import / 格式化。|

### 测试 & 质量
- `mvn test` 运行单元/集成测试（Surefire 匹配 `*Test.java`、`*Tests.java`）。
- 使用 IDE 自动格式化或 `mvn fmt` / Spotless，保持 Java 21 兼容。
- 涉及安全、支付、Yjs 的改动需附回归说明与文档链接。

### 文档索引
- `docs/solution-2-nodejs-microservice.md`：Yjs 微服务方案。
- 上级仓库 `../docs/*.md`：云存储、WebSocket、运维等专题。

### 贡献指南
1. 分支命名：`feature/*`、`fix/*`、`chore/*`。
2. 新模块遵循按域划分的 package 结构，保持依赖单向。
3. 新配置项需更新 `application*.yml` 注释及 Docker Compose 环境变量说明。
4. Schema 或 API 变更需附 Flyway 迁移、测试覆盖与文档更新。

---

## 🇺🇸 English Version

> Spring Boot 3 / Java 21 implementation of the Yunke collaborative backend. It inherits the proven AFFiNE foundations but focuses exclusively on the Java stack: multi-tenant workspaces, CRDT orchestration, payments, AI, notifications, community features, and observability tooling.

```
Browser / Client → REST & Socket.IO (8080 / 9092)
                    ↓
        Controller & DTO Layer
                    ↓
           Service / Domain Layer
      (workspace, document, payment…)
                    ↓
    Repository Layer (JPA, Redis, Yjs)
                    ↓
MySQL 8  |  Redis 7  |  Yjs Node Service
```

### Key Capabilities
- Workspace & document graph with RBAC/ACL enforcement plus CRDT state.
- CRDT integration via the dedicated Yjs Node microservice (merge/diff/state-vector/batch/validate).
- Realtime messaging through Socket.IO/Netty WebSocket (`9092`) alongside async notifications, email, community/forum modules.
- Monetization-ready payment abstraction (Alipay sandbox by default) covering callbacks and storefront flows.
- Ops tooling: Actuator endpoints, Prometheus config, structured logs, dependency analyzer, RBAC smoke tests, daemon scripts.

### Project Layout
| Path | Purpose |
| --- | --- |
| `src/main/java/com/yunke/backend/` | Feature packages (`user`, `workspace`, `document`, `community`, `forum`, `notification`, `payment`, `ai`, `storage`, `system`, `security`, `infrastructure`). |
| `src/main/resources/` | `application*.yml`, Flyway migrations (`db/migration`), logback config, static assets/templates. |
| `docs/` | Design notes (e.g., `solution-2-nodejs-microservice.md`). |
| `docker-compose.yml` | Reference stack (Java backend + Yjs + MySQL + Redis). |
| `prometheus.yml` | Sample scrape configuration. |
| `start.sh`, `test-role-system.sh`, `analyze_dependencies.sh`, `update_imports_batch.sh` | Operational helpers. |
| `storage/`, `uploads/`, `logs/` | Local storage mounts and log destinations. |

### Module Guide
`admin`, `ai`, `common`, `community`, `forum`, `document`, `infrastructure`, `notification`, `payment`, `search`, `security`, `storage`, `system`, `user`, `workspace` — each encapsulates its aggregate/domain.

### Configuration
- Profiles: `dev`, `test`, `prod` (set `SPRING_PROFILES_ACTIVE`).
- Primary config: `application.yml` (MySQL, Redis, Flyway, logging, Socket.IO/WebSocket, Yjs endpoints).
- Overrides: `application-security.yml`, `application-performance.yml`, `application-server*.properties`; create `application.local.yml` for secrets.
- Key properties: `spring.datasource.*`, `spring.redis.*`, `yjs.service.url`, `affine.payment.*`, `affine.storage.*`, `socketio.server.*`, `websocket.*`.

### External Dependencies
| Service | Default | Notes |
| --- | --- | --- |
| MySQL 8.0 | `localhost:3306`, db `affine`, user `root/root` | Flyway scripts under `classpath:db/migration`; SQL dumps in `../database/`. |
| Redis 7 | `localhost:6379` | Cache, session, pub/sub. |
| Yjs Node Service | `http://localhost:3001` | Required for all CRDT APIs. |

### Running Locally
1. Prereqs: JDK 21, Maven 3.9+, MySQL 8, Redis 7, Node 18 (Yjs). Import `../database/affine.sql` if needed.
2. Configure: `cp src/main/resources/application.yml src/main/resources/application.local.yml` and edit.
3. Build: `mvn clean package -DskipTests`
4. Run: `java -jar target/yunke-java-backend-0.21.0.jar --spring.profiles.active=dev`
   - REST: `http://localhost:8080`
   - Socket.IO/WebSocket: `ws://localhost:9092`
5. Docker Compose: `docker compose up -d` (services: `yjs-service`, `affine-backend`, `mysql`, `redis`; override envs as needed).

### Database & Migration
- Flyway executes on startup; place scripts in `src/main/resources/db/migration`.
- Manual SQL utilities: `../database/` (baseline, patches, fixes).
- Schema changes must ship migrations, dump updates, and coordination notes.

### Observability & Ops
- Actuator: `/actuator/health`, `/info`, `/metrics`, `/prometheus`.
- Prometheus: see `prometheus.yml` for scraping `http://<host>:8080/actuator/prometheus`.
- Logging: `logback-spring.xml` → `logs/affine-backend.log`.
- Health probes: target `/actuator/health` in Docker/K8s.

### Scripts & Tooling
`start.sh`, `test-role-system.sh`, `analyze_dependencies.sh`, `update_imports_batch.sh` handle daemon runs, RBAC validation, dependency audits, and formatting cleanup.

### Testing & Quality
- `mvn test` (Surefire picks up `*Test.java`, `*Tests.java`).
- Format via IDE or `mvn fmt` / Spotless; keep Java 21 compatibility.
- Changes touching security/payment/Yjs require regression notes plus doc references.

### Documentation Pointers
- `docs/solution-2-nodejs-microservice.md`: Yjs microservice decisions.
- Parent repo `../docs/*.md`: cloud-save architecture, websocket tuning, ops handbook, etc.

### Contribution Guidelines
1. Branch naming: `feature/*`, `fix/*`, `chore/*`.
2. Follow the feature-package structure for new modules.
3. Document new config knobs in `application*.yml` comments and Docker Compose env hints.
4. Include migrations, tests, and documentation for schema or API changes.

