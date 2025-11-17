# 配置指南

## 配置文件

### Profile 环境
- `dev`（默认）：开发环境
- `test`：测试环境
- `prod`：生产环境

通过环境变量切换：
```bash
SPRING_PROFILES_ACTIVE=prod
```

### 主配置文件

| 文件 | 用途 |
|------|------|
| `application.yml` | 主配置（数据库、Redis、日志等） |
| `application-security.yml` | 安全相关配置 |
| `application-performance.yml` | 性能优化配置 |
| `application-server*.properties` | 服务器配置 |
| `application.local.yml` | 本地私密配置（需自建，已忽略） |

### 关键属性

```yaml
# 数据库
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/affine
    username: root
    password: root

# Redis
  redis:
    host: localhost
    port: 6379

# Yjs 微服务
yjs:
  service:
    url: http://localhost:3001

# 支付
affine:
  payment:
    provider: alipay
    sandbox: true

# 存储
  storage:
    type: local
    path: ./storage

# Socket.IO
socketio:
  server:
    port: 9092
```

## 外部依赖

| 服务 | 默认地址 | 说明 |
|------|----------|------|
| MySQL 8.0 | `localhost:3306` | 数据库 `affine`，用户 `root` |
| Redis 7 | `localhost:6379` | 缓存、会话、Pub/Sub |
| Yjs Node Service | `http://localhost:3001` | CRDT 操作微服务 |

## 数据库配置

### Flyway 迁移
- 启动时自动执行
- 脚本位置：`src/main/resources/db/migration`
- 命名规范：`V{version}__{description}.sql`

### 手动 SQL
- 基线脚本：`../database/affine.sql`
- 补丁脚本：`../database/patches/`
- 修复脚本：`../database/fixes/`

## Docker Compose 环境变量

```yaml
environment:
  - SPRING_PROFILES_ACTIVE=prod
  - MYSQL_HOST=mysql
  - MYSQL_PORT=3306
  - MYSQL_DATABASE=affine
  - MYSQL_USER=root
  - MYSQL_PASSWORD=root
  - REDIS_HOST=redis
  - REDIS_PORT=6379
  - YJS_SERVICE_URL=http://yjs-service:3001
```

## 安全配置

### JWT 设置
```yaml
jwt:
  secret: your-secret-key
  expiration: 86400000  # 24小时
  refresh-expiration: 604800000  # 7天
```

### CORS 配置
```yaml
cors:
  allowed-origins:
    - http://localhost:3000
    - https://your-domain.com
  allowed-methods: GET,POST,PUT,DELETE,OPTIONS
```

## 存储配置

### 本地存储
```yaml
affine:
  storage:
    type: local
    path: ./storage
    max-size: 10GB
```

### S3/OSS 存储
```yaml
affine:
  storage:
    type: s3
    endpoint: https://s3.amazonaws.com
    bucket: your-bucket
    access-key: xxx
    secret-key: xxx
```

## 日志配置

配置文件：`src/main/resources/logback-spring.xml`

输出位置：`logs/affine-backend.log`

日志级别：
```yaml
logging:
  level:
    root: INFO
    com.yunke.backend: DEBUG
    org.springframework: INFO
```
