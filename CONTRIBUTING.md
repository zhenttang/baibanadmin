# 贡献指南

感谢您对云科后端项目的关注！本文档将帮助您了解如何参与项目开发。

## 开发环境

### 必需工具
- JDK 21
- Maven 3.9+
- Git
- IDE（推荐 IntelliJ IDEA）

### 代码规范
- 使用 IDE 自动格式化或 `mvn fmt` / Spotless
- 保持 Java 21 兼容性
- 遵循项目现有代码风格

## 分支管理

### 命名规范
- `feature/*` - 新功能
- `fix/*` - Bug 修复
- `chore/*` - 杂项（依赖更新、配置调整等）
- `docs/*` - 文档更新

### 工作流程
```bash
# 1. 创建分支
git checkout -b feature/your-feature

# 2. 开发并提交
git add .
git commit -m "feat: 添加新功能"

# 3. 推送并创建 PR
git push origin feature/your-feature
```

## 项目结构

### 按域划分的包结构
```
src/main/java/com/yunke/backend/
├── admin/          # 管理端接口
├── ai/             # AI 助手
├── common/         # 公共组件
├── community/      # 社区功能
├── document/       # 文档管理
├── forum/          # 论坛模块
├── infrastructure/ # 基础设施
├── notification/   # 通知服务
├── payment/        # 支付模块
├── search/         # 搜索功能
├── security/       # 安全认证
├── storage/        # 存储服务
├── system/         # 系统设置
├── user/           # 用户管理
└── workspace/      # 工作空间
```

### 新模块要求
1. 遵循按域划分的 package 结构
2. 保持依赖单向（避免循环依赖）
3. 包含完整的 Controller → Service → Repository 分层

## 提交规范

### Commit Message 格式
```
<type>(<scope>): <subject>

<body>

<footer>
```

### Type 类型
- `feat`: 新功能
- `fix`: Bug 修复
- `docs`: 文档更新
- `style`: 代码格式（不影响功能）
- `refactor`: 重构
- `test`: 测试相关
- `chore`: 构建/工具变动

### 示例
```
feat(document): 添加文档版本历史功能

- 实现版本快照存储
- 添加版本对比接口
- 支持版本回滚

Closes #123
```

## 测试要求

### 运行测试
```bash
mvn test
```

### 测试规范
- 单元测试：`*Test.java`
- 集成测试：`*Tests.java`
- 测试覆盖率建议 >70%

### 特殊模块
涉及以下模块的改动需附回归说明与文档链接：
- 安全认证
- 支付功能
- Yjs 集成

## 配置变更

### 新配置项
1. 更新 `application*.yml` 添加注释说明
2. 更新 Docker Compose 环境变量
3. 在配置文档中说明用途

### 示例
```yaml
# application.yml
your-module:
  enabled: true          # 是否启用
  timeout: 30000         # 超时时间（毫秒）
  max-retries: 3         # 最大重试次数
```

## 数据库变更

### Schema 变更流程
1. 编写 Flyway 迁移脚本
   - 位置：`src/main/resources/db/migration`
   - 命名：`V{version}__{description}.sql`
2. 更新 SQL dump 文档
3. 添加测试覆盖
4. 更新相关文档

### 迁移脚本示例
```sql
-- V1_3__Add_user_preferences_table.sql
CREATE TABLE user_preferences (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    preference_key VARCHAR(100) NOT NULL,
    preference_value TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id)
);
```

## API 变更

### 新增 API
1. 遵循 RESTful 规范
2. 添加 Swagger/OpenAPI 注释
3. 编写接口文档
4. 添加集成测试

### 修改 API
1. 考虑向后兼容
2. 标记废弃 API（`@Deprecated`）
3. 提供迁移指南
4. 更新 API 文档

## Pull Request

### PR 要求
- 清晰的标题和描述
- 关联相关 Issue
- 通过所有 CI 检查
- 至少一位 Reviewer 批准

### PR 模板
```markdown
## 变更说明
简要描述本次变更内容

## 变更类型
- [ ] 新功能
- [ ] Bug 修复
- [ ] 文档更新
- [ ] 重构
- [ ] 其他

## 测试
- [ ] 添加/更新了单元测试
- [ ] 本地测试通过
- [ ] 不需要测试

## 检查清单
- [ ] 代码遵循项目规范
- [ ] 更新了相关文档
- [ ] 无破坏性变更（或已说明）
```

## 获取帮助

- 查阅 `docs/` 目录下的技术文档
- 在 Issue 中提问
- 联系项目维护者

## 行为准则

- 尊重所有贡献者
- 提供建设性反馈
- 保持专业和友好
