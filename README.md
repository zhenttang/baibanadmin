# 云科（YUNKE）协作白板后端

<p align="center">
  <img src="assets/yunke-logo.png" width="180" alt="YUNKE Logo" />
</p>

> 基于 AFFiNE 开源后端演进并针对云科（YUNKE）业务场景深度重构的协作白板服务集。仓库同时托管 Java + Go 两套服务、数据库脚本以及运行文档，用于支撑多租户文档、实时协作、社区与支付等能力。

---

## 中文版

### 概览 Highlights
- **双栈服务**：`yunke-java-backend`（Spring Boot 3 + Java 21）承载核心业务域，`yunke-go-backend`（Gin + Go 1.21）提供实时/网关增强，可独立部署或协同运行。
- **CRDT 能力复用**：集成参考 AFFiNE 的 Yjs Node 微服务，统一处理文档合并、差异与状态向量，保障多端 100% 兼容。
- **可观测性内建**：Spring Actuator、Prometheus 指标、结构化日志脚本 ready，辅以 `prometheus.yml` 与 `docs/ops-handbook.md`。
- **数据库资产**：`database/` 提供 AFFiNE 原始 schema、补丁及常用修复 SQL；Java 服务内置 Flyway 迁移。
- **脚本化运维**：`start.sh`、`docker-compose.yml`、`test-role-system.sh`、`analyze_dependencies.sh` 等脚本覆盖本地与 CI/CD 场景。

### 仓库结构 Repository Map
| 路径 | 说明 |
| --- | --- |
| `yunke-java-backend/` | Spring Boot 主后端，模块涵盖用户、工作区、文档、AI、支付、通知、社区、论坛等，整合 MySQL + Redis + Yjs。|
| `yunke-go-backend/` | Go 语言实现的轻量/实时服务，包含 WebSocket、Yjs 客户端、AI、支付、社区模块（详见子仓 README）。|
| `database/` | 初始化/修复 SQL（如 `affine.sql`、`add_deleted_at_to_all_tables.sql`）。|
| `docs/` | 架构说明、运维手册、Yjs 设计、WebSocket 修复等专题文档。|
| `assets/` | 品牌与图标资源（例如 `yunke-logo.png`）。|

> 其他子目录（如 `docs/workspace-doc-alignment/`）存放专题调研，便于排查真实业务问题。

### 技术栈 Tech Stack
- **Java 服务**：Spring Boot 3.2、Spring Security、JPA/Hibernate、Flyway、Redis、Micrometer、JWT、Socket.IO + Netty WebSocket。
- **Go 服务**：Gin、GORM、Zap、Redis、JWT、Yjs Client、WebSocket。
- **协作引擎**：Yjs 13.x Node 微服务（可直接复用 `../yunkeYJS` 仓库）。
- **基础设施**：MySQL 8.0、Redis 7、Docker Compose、Prometheus。

### 服务拆解 Service Overview
#### `yunke-java-backend`
- 入口：`com.yunke.backend.YunkeApplication`
- 核心包：`user`（认证/角色）、`workspace`（空间/权限）、`document`（CRDT 文档 + 历史）、`community`、`forum`、`notification`、`payment`、`ai` 等。
- 配置：`src/main/resources/application*.yml` 划分 dev/test/prod，另有 `application-security.yml`、`application-performance.yml`。
- 外部依赖：MySQL、Redis、Yjs Service、(可选) 对象存储。

#### `yunke-go-backend`
- 入口：`cmd/server/main.go`
- 模块：用户、工作区、文档、支付、AI、社区、通知、存储等封装在 `internal/`。
- 配置：`configs/config.yaml` → 建议复制为 `config.local.yaml` 并按 README 配置。
- 主要场景：轻量 API、WebSocket/Socket.IO 桥接、实验性服务。

### 快速开始 Quick Start
#### 1. 依赖准备
| 组件 | 版本建议 |
| --- | --- |
| JDK | 21+ |
| Maven | 3.9+ |
| Go | 1.21+ （如需运行 Go 服务）|
| Node.js | 18+ （运行 Yjs 微服务）|
| Docker / Docker Compose | 可选，用于一键启动依赖 |
| MySQL / Redis | 若不使用 Compose 需本地安装 |

#### 2. 克隆与公共配置
```bash
git clone <repo>
cd baibanhouduan
```
- 数据库：导入 `database/affine.sql`（或 `affine_mysql57.sql` 兼容低版本）。
- 需要额外字段时执行 `add_deleted_at_to_all_tables.sql` 等补丁脚本。

#### 3. 启动 Java 后端
```bash
cd yunke-java-backend
cp src/main/resources/application.yml src/main/resources/application.local.yml  # 可选
# 修改数据库、Redis、Yjs、支付、存储等配置
mvn clean package -DskipTests
java -jar target/yunke-java-backend-0.21.0.jar --spring.profiles.active=dev
```
默认监听 `http://localhost:8080`，Socket.IO/WebSocket 端口 `9092`。

**Docker Compose**
```bash
cd yunke-java-backend
docker compose up -d
```
依次启动：Yjs 微服务（需准备 `./yjs-service`）、Spring Boot 服务、MySQL、Redis。

常用环境变量：`SPRING_PROFILES_ACTIVE`、`YJS_SERVICE_URL`、`MYSQL_*`、`REDIS_HOST`。

#### 4. 启动 Go 后端
```bash
cd yunke-go-backend
cp configs/config.yaml configs/config.local.yaml
# 编辑数据库、Redis、Yjs、存储配置
go run cmd/server/main.go
```
更多细节参考 `yunke-go-backend/README.md` 与 `START_GUIDE.md`。

#### 5. 集成 Yjs 微服务
- 建议直接使用 `../yunkeYJS`（Node.js + Express + 官方 Yjs 13.x）。
- Java/Go 服务通过 `YjsServiceClient` 或 `pkg/yjs` 调用 `/api/yjs/*`（create-empty/merge/diff/state-vector/batch/validate）。
- 所有二进制 CRDT 操作必须由该微服务处理，避免在主语言端手写更新。

### 数据库与迁移
- **Flyway**：Java 服务启动自动执行 `classpath:db/migration`；通过 `SPRING_PROFILES_ACTIVE` 控制行为。
- **SQL 脚本**： `database/affine.sql`（基础 schema）、`add_deleted_at_simple.sql`、`fix_snapshot_histories.sql`、`check_updates.sql`（Go 子仓）。
- 导入建议：使用 `mysql --default-character-set=utf8mb4`，防止编码问题。

### 观测与运维
- **Actuator**：`/actuator/health`、`/actuator/info`、`/actuator/metrics`。
- **Prometheus**：参考 `yunke-java-backend/prometheus.yml` 抓取 `http://<host>:8080/actuator/prometheus`。
- **日志**：Logback 输出 `logs/affine-backend.log`；Go 服务使用 Zap JSON。
- **脚本**：`start.sh`（守护 Java）、`test-role-system.sh`（校验权限）、`analyze_dependencies.sh`（依赖树）、`update_imports_batch.sh`（批量格式化）。

### 文档索引 Docs
| 文件 | 内容 |
| --- | --- |
| `docs/cloud-save-architecture.md` | 云端存储/保存流程方案。|
| `docs/ops-handbook.md` | 运维检查清单、日志定位、常见告警。|
| `docs/yjs-architecture.md` & `yjs-implementation-summary.md` | Yjs 服务集成方案与演进记录。|
| `docs/duplicate-key-fix.md` | 数据冲突修复指南。|
| `docs/websocket-frame-size-fix.md` | 大帧传输调优记录。|
| `docs/workspace-doc-alignment/` | 文档与工作区对齐策略调研。|

### 常见问题 FAQ
1. **为什么仍然出现 AFFiNE 前缀？** 为继承原有数据结构与兼容旧客户端，可逐步改名，但需同步数据库与 Yjs 元数据。
2. **Yjs 服务必须在哪？** 可本地或内网部署，只要 `YJS_SERVICE_URL` 可访问且 `/health` 正常。
3. **是否必须同时启动 Java 与 Go？** 否，可按业务拆分；Java 负责核心 API，Go 可作为实时网关或实验性服务。

### 贡献指南 Contributing
1. Git 分支命名：`feature/*`、`fix/*`、`chore/*`。
2. Java：`mvn fmt` + `mvn -pl :yunke-java-backend test`；Go：`golangci-lint run`（若安装）。
3. 变更数据库 schema 需同步更新 `database/` SQL 与 Java `db/migration`。
4. 涉及 Yjs 协议的改动需更新 `docs/yjs-*.md` 并在 PR 中附带回归计划。

---

## English Version

### Overview
- **Dual runtime**: `yunke-java-backend` (Spring Boot 3 / Java 21) owns the mission‑critical domains, while `yunke-go-backend` (Gin / Go 1.21) provides lightweight + realtime services.
- **CRDT via Yjs**: A Node.js microservice (ported from AFFiNE) handles every doc merge/diff/state-vector operation to guarantee binary compatibility.
- **Ops ready**: Actuator endpoints, Prometheus scraping config, structured logs, and helper scripts (`start.sh`, `analyze_dependencies.sh`, etc.).
- **Database assets**: `database/` ships the original AFFiNE schema plus common fixes; Flyway migrations live inside the Java service.

### Repository Layout
| Path | What lives here |
| --- | --- |
| `yunke-java-backend/` | Main Spring Boot service (users, workspaces, docs, AI, payments, community, forums, notifications, storage…). |
| `yunke-go-backend/` | Go service focusing on realtime bridges, Socket.IO/WebSocket gateways, experimental APIs. |
| `database/` | SQL dumps & patches (`affine.sql`, `add_deleted_at_to_all_tables.sql`, etc.). |
| `docs/` | Architecture notes, ops handbook, Yjs deep dives, websocket fixes. |
| `assets/` | Branding such as `yunke-logo.png`. |

### Tech Stack
- Java: Spring Boot 3.2, Spring Security, Hibernate/JPA, Flyway, Redis, Micrometer, JWT, Socket.IO + Netty WebSocket.
- Go: Gin, GORM, Zap, Redis, JWT, pkg/yjs client, WebSocket.
- Collaboration engine: Yjs 13.x Node microservice (see `../yunkeYJS`).
- Infra: MySQL 8, Redis 7, Docker Compose, Prometheus.

### Getting Started
1. **Prerequisites**: JDK 21, Maven 3.9+, Go 1.21+, Node 18+, locally installed MySQL/Redis or Docker Compose.
2. **Clone + DB**: `git clone <repo> && cd baibanhouduan`; import `database/affine.sql` (use `*_mysql57.sql` for legacy environments) and apply optional patches.
3. **Run Java backend**:
   ```bash
   cd yunke-java-backend
   mvn clean package -DskipTests
   java -jar target/yunke-java-backend-0.21.0.jar --spring.profiles.active=dev
   ```
   or `docker compose up -d` to boot the stack (Yjs + backend + MySQL + Redis).
4. **Run Go backend**:
   ```bash
   cd yunke-go-backend
   cp configs/config.yaml configs/config.local.yaml
   go run cmd/server/main.go
   ```
5. **Run Yjs service**: start `../yunkeYJS` (Node 18+) and set `YJS_SERVICE_URL` accordingly. Never craft CRDT binaries in Java/Go directly.

### Database & Migration
- Flyway migrates schemas from `src/main/resources/db/migration` on startup.
- Manual SQLs live under `database/` for quick replays or hot fixes.
- Import using UTF‑8 (`mysql --default-character-set=utf8mb4`) to avoid encoding issues.

### Observability & Ops
- Actuator endpoints exposed on `/actuator/*` (health/info/metrics/prometheus).
- `prometheus.yml` shows how to scrape the Java backend.
- Logs: Logback → `logs/affine-backend.log`; Go → Zap JSON streams.
- Helper scripts: `start.sh`, `test-role-system.sh`, `analyze_dependencies.sh`, `update_imports_batch.sh`.

### Documentation Index
`docs/cloud-save-architecture.md`, `docs/ops-handbook.md`, `docs/yjs-architecture.md`, `docs/yjs-implementation-summary.md`, `docs/duplicate-key-fix.md`, `docs/websocket-frame-size-fix.md`, plus the `workspace-doc-alignment/` research notes.

### FAQ
1. **Why do AFFiNE names still exist?** Legacy schema/client compatibility; rename gradually with DB + Yjs metadata updates.
2. **Is the Yjs service mandatory?** Yes for any CRDT operations—host it locally or in the same VPC and keep `/health` reachable.
3. **Do I need both Java & Go services online?** No. Deploy the runtime that fits the workload; the other can remain optional or act as a realtime edge.

### Contributing
- Branch naming: `feature/*`, `fix/*`, `chore/*`.
- Java checks: `mvn fmt`, `mvn -pl :yunke-java-backend test`.
- Go checks: `golangci-lint run` (when available).
- Schema or Yjs protocol changes must update both SQL assets and docs under `docs/yjs-*.md` together with regression notes.

---
The README will continue to evolve；如需更多中文/英文专题，可在 `docs/` 新建文件或提交 Issue。
