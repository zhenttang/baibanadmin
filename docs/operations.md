# 运维手册

## 本地运行

### 环境要求
- JDK 21
- Maven 3.9+
- MySQL 8.0
- Redis 7
- Node.js 18（Yjs 微服务）

### 启动步骤

1. **准备数据库**
```bash
mysql -u root -p < ../database/affine.sql
```

2. **配置应用**
```bash
cd yunke-java-backend
cp src/main/resources/application.yml src/main/resources/application.local.yml
# 编辑 application.local.yml 填入实际配置
```

3. **构建项目**
```bash
mvn clean package -DskipTests
```

4. **启动服务**
```bash
java -jar target/yunke-java-backend-0.21.0.jar --spring.profiles.active=dev
```

服务地址：
- REST API: `http://localhost:8080`
- WebSocket: `ws://localhost:9092`

## Docker 部署

### 使用 Docker Compose
```bash
docker compose up -d
```

包含服务：
- `affine-backend` - Java 后端
- `yjs-service` - Yjs 微服务
- `mysql` - 数据库
- `redis` - 缓存

### 健康检查
```bash
docker compose ps
docker compose logs -f affine-backend
```

## 监控与观测

### Actuator 端点

| 端点 | 用途 |
|------|------|
| `/actuator/health` | 健康检查 |
| `/actuator/info` | 应用信息 |
| `/actuator/metrics` | 性能指标 |
| `/actuator/prometheus` | Prometheus 指标 |

### Prometheus 集成

参考配置：`prometheus.yml`

```yaml
scrape_configs:
  - job_name: 'yunke-backend'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['localhost:8080']
```

### 日志管理

配置文件：`logback-spring.xml`

日志位置：`logs/affine-backend.log`

日志轮转：
- 单文件最大 100MB
- 保留 30 天历史
- 压缩归档

## 脚本工具

| 脚本 | 功能 |
|------|------|
| `start.sh` | 守护进程方式启动 |
| `test-role-system.sh` | 校验角色权限矩阵 |
| `analyze_dependencies.sh` | 分析 Maven 依赖树 |
| `update_imports_batch.sh` | 批量整理 import 语句 |

### 使用示例

```bash
# 守护进程启动
./start.sh

# 检查角色系统
./test-role-system.sh

# 分析依赖
./analyze_dependencies.sh > dependencies.txt
```

## 数据库维护

### Flyway 迁移
- 自动执行：启动时检查并执行新迁移
- 手动执行：`mvn flyway:migrate`
- 查看状态：`mvn flyway:info`

### 备份策略
```bash
# 全量备份
mysqldump -u root -p affine > backup_$(date +%Y%m%d).sql

# 增量备份（建议使用 binlog）
```

### Schema 变更流程
1. 编写迁移脚本 `V{version}__{description}.sql`
2. 更新 SQL dump 文档
3. 通知相关依赖方
4. 代码审查后合并

## 故障排查

### 常见问题

**启动失败**
```bash
# 检查端口占用
netstat -tlnp | grep 8080
netstat -tlnp | grep 9092

# 检查数据库连接
mysql -u root -p -h localhost affine
```

**内存不足**
```bash
# 调整 JVM 参数
java -Xms512m -Xmx2g -jar target/yunke-java-backend-0.21.0.jar
```

**Redis 连接失败**
```bash
redis-cli ping
redis-cli info
```

### 日志分析
```bash
# 查看错误日志
grep -i error logs/affine-backend.log

# 查看最近日志
tail -f logs/affine-backend.log
```

## 性能优化

### JVM 参数
```bash
java -server \
  -Xms1g -Xmx4g \
  -XX:+UseG1GC \
  -XX:MaxGCPauseMillis=200 \
  -jar target/yunke-java-backend-0.21.0.jar
```

### 数据库优化
- 索引优化
- 查询缓存
- 连接池调优

### Redis 优化
- 内存策略
- 持久化配置
- 集群部署

## 安全建议

- 定期更新依赖包
- 启用 HTTPS
- 配置防火墙规则
- 启用审计日志
- 定期安全扫描
