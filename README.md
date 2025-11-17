<p align="center">
  <img src="assets/yunke-logo.png" alt="云科 Logo" width="120" height="120">
</p>

<h1 align="center">云科 (YUNKE) Java Backend</h1>

<p align="center">
  <strong>下一代协同办公平台的 Java 后端服务</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-green?style=flat-square&logo=springboot" alt="Spring Boot 3">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/PRs-welcome-brightgreen?style=flat-square" alt="PRs Welcome">
</p>

<p align="center">
  <a href="#-特性亮点">特性</a> •
  <a href="#-快速开始">快速开始</a> •
  <a href="#-架构概览">架构</a> •
  <a href="#-文档">文档</a> •
  <a href="./README_EN.md">English</a>
</p>

---

## ✨ 特性亮点

<table>
  <tr>
    <td align="center" width="25%">
      <img src="https://img.icons8.com/fluency/48/000000/workstation.png" alt="Workspace">
      <br><strong>多租户工作空间</strong>
      <br><sub>组织→空间→文档层级<br>内建 RBAC/ACL 权限控制</sub>
    </td>
    <td align="center" width="25%">
      <img src="https://img.icons8.com/fluency/48/000000/synchronize.png" alt="CRDT">
      <br><strong>实时协同编辑</strong>
      <br><sub>基于 CRDT 的文档同步<br>毫秒级状态一致性</sub>
    </td>
    <td align="center" width="25%">
      <img src="https://img.icons8.com/fluency/48/000000/artificial-intelligence.png" alt="AI">
      <br><strong>AI 智能助手</strong>
      <br><sub>多模型支持<br>智能内容生成与分析</sub>
    </td>
    <td align="center" width="25%">
      <img src="https://img.icons8.com/fluency/48/000000/online-payment.png" alt="Payment">
      <br><strong>支付系统</strong>
      <br><sub>可插拔支付提供商<br>订阅与计费管理</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://img.icons8.com/fluency/48/000000/conference-call.png" alt="Community">
      <br><strong>社区生态</strong>
      <br><sub>内容分享与互动<br>论坛与评论系统</sub>
    </td>
    <td align="center">
      <img src="https://img.icons8.com/fluency/48/000000/appointment-reminders.png" alt="Notification">
      <br><strong>多渠道通知</strong>
      <br><sub>邮件/SMS/Push<br>智能消息推送</sub>
    </td>
    <td align="center">
      <img src="https://img.icons8.com/fluency/48/000000/cloud-storage.png" alt="Storage">
      <br><strong>灵活存储</strong>
      <br><sub>本地/S3/OSS<br>多策略文件管理</sub>
    </td>
    <td align="center">
      <img src="https://img.icons8.com/fluency/48/000000/monitor.png" alt="Observability">
      <br><strong>可观测性</strong>
      <br><sub>Prometheus + Actuator<br>全方位监控告警</sub>
    </td>
  </tr>
</table>

---

## 🚀 快速开始

### 环境要求

- JDK 21
- Maven 3.9+
- MySQL 8.0
- Redis 7
- Node.js 18 (Yjs 微服务)

### 一键启动 (Docker)

```bash
docker compose up -d
```

### 手动启动

```bash
# 1. 克隆项目
git clone https://github.com/your-org/yunke-java-backend.git
cd yunke-java-backend

# 2. 构建
mvn clean package -DskipTests

# 3. 运行
java -jar target/yunke-java-backend-0.21.0.jar
```

**访问地址：**
- REST API: http://localhost:8080
- WebSocket: ws://localhost:9092
- 健康检查: http://localhost:8080/actuator/health

---

## 🏗️ 架构概览

<p align="center">
  <img src="assets/architecture.svg" alt="系统架构图" width="100%">
</p>

### 核心模块

| 模块 | 职责 |
|------|------|
| `workspace` | 工作空间与权限管理 |
| `document` | 文档元数据与协同 |
| `payment` | 支付抽象与订单 |
| `ai` | AI 助手与模型集成 |
| `community` | 社区内容与互动 |
| `notification` | 多渠道消息推送 |
| `security` | JWT 认证与授权 |
| `storage` | 文件存储策略 |

---

## 📚 文档

| 文档 | 说明 |
|------|------|
| [配置指南](./docs/configuration.md) | 环境配置与参数说明 |
| [运维手册](./docs/operations.md) | 部署、监控与故障排查 |
| [贡献指南](./CONTRIBUTING.md) | 开发规范与提交流程 |
| [Yjs 微服务方案](./docs/solution-2-nodejs-microservice.md) | CRDT 架构设计 |

---

## 🛠️ 技术栈

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL">
  <img src="https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis">
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker">
</p>

---

## 📊 项目状态

- 版本：`0.21.0`
- Java：`21`
- Spring Boot：`3.x`
- 状态：积极开发中

---

## 🤝 参与贡献

欢迎提交 Issue 和 Pull Request！请先阅读 [贡献指南](./CONTRIBUTING.md)。

```bash
# 分支命名
feature/your-feature
fix/bug-description
docs/documentation-update
```

---

## 📄 许可证

本项目基于 MIT 许可证开源 - 详见 [LICENSE](LICENSE) 文件。

---

<p align="center">
  Made with ❤️ by YUNKE Team
</p>
