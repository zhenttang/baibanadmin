<p align="center">
  <img src="assets/yunke-logo.png" alt="YUNKE Logo" width="120" height="120">
</p>

<h1 align="center">YUNKE Java Backend</h1>

<p align="center">
  <strong>Next-generation collaborative workspace platform backend service</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk" alt="Java 21">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-green?style=flat-square&logo=springboot" alt="Spring Boot 3">
  <img src="https://img.shields.io/badge/License-MIT-blue?style=flat-square" alt="License">
  <img src="https://img.shields.io/badge/PRs-welcome-brightgreen?style=flat-square" alt="PRs Welcome">
</p>

<p align="center">
  <a href="#-key-features">Features</a> •
  <a href="#-quick-start">Quick Start</a> •
  <a href="#-architecture">Architecture</a> •
  <a href="#-documentation">Docs</a> •
  <a href="./README.md">中文</a>
</p>

---

## ✨ Key Features

<table>
  <tr>
    <td align="center" width="25%">
      <img src="https://img.icons8.com/fluency/48/000000/workstation.png" alt="Workspace">
      <br><strong>Multi-tenant Workspaces</strong>
      <br><sub>Organization→Space→Document hierarchy<br>Built-in RBAC/ACL</sub>
    </td>
    <td align="center" width="25%">
      <img src="https://img.icons8.com/fluency/48/000000/synchronize.png" alt="CRDT">
      <br><strong>Real-time Collaboration</strong>
      <br><sub>CRDT-based document sync<br>Millisecond consistency</sub>
    </td>
    <td align="center" width="25%">
      <img src="https://img.icons8.com/fluency/48/000000/artificial-intelligence.png" alt="AI">
      <br><strong>AI Assistant</strong>
      <br><sub>Multi-model support<br>Intelligent content generation</sub>
    </td>
    <td align="center" width="25%">
      <img src="https://img.icons8.com/fluency/48/000000/online-payment.png" alt="Payment">
      <br><strong>Payment System</strong>
      <br><sub>Pluggable payment providers<br>Subscription & billing</sub>
    </td>
  </tr>
  <tr>
    <td align="center">
      <img src="https://img.icons8.com/fluency/48/000000/conference-call.png" alt="Community">
      <br><strong>Community Ecosystem</strong>
      <br><sub>Content sharing & interaction<br>Forum & comments</sub>
    </td>
    <td align="center">
      <img src="https://img.icons8.com/fluency/48/000000/appointment-reminders.png" alt="Notification">
      <br><strong>Multi-channel Notifications</strong>
      <br><sub>Email/SMS/Push<br>Smart message delivery</sub>
    </td>
    <td align="center">
      <img src="https://img.icons8.com/fluency/48/000000/cloud-storage.png" alt="Storage">
      <br><strong>Flexible Storage</strong>
      <br><sub>Local/S3/OSS<br>Multi-strategy file management</sub>
    </td>
    <td align="center">
      <img src="https://img.icons8.com/fluency/48/000000/monitor.png" alt="Observability">
      <br><strong>Observability</strong>
      <br><sub>Prometheus + Actuator<br>Comprehensive monitoring</sub>
    </td>
  </tr>
</table>

---

## 🚀 Quick Start

### Prerequisites

- JDK 21
- Maven 3.9+
- MySQL 8.0
- Redis 7
- Node.js 18 (for Yjs microservice)

### One-click Start (Docker)

```bash
docker compose up -d
```

### Manual Setup

```bash
# 1. Clone the repository
git clone https://github.com/your-org/yunke-java-backend.git
cd yunke-java-backend

# 2. Build
mvn clean package -DskipTests

# 3. Run
java -jar target/yunke-java-backend-0.21.0.jar
```

**Access Points:**
- REST API: http://localhost:8080
- WebSocket: ws://localhost:9092
- Health Check: http://localhost:8080/actuator/health

---

## 🏗️ Architecture

<p align="center">
  <img src="assets/architecture.svg" alt="System Architecture" width="100%">
</p>

### Core Modules

| Module | Responsibility |
|--------|----------------|
| `workspace` | Workspace & permission management |
| `document` | Document metadata & collaboration |
| `payment` | Payment abstraction & orders |
| `ai` | AI assistant & model integration |
| `community` | Community content & interaction |
| `notification` | Multi-channel messaging |
| `security` | JWT authentication & authorization |
| `storage` | File storage strategies |

---

## 📚 Documentation

| Document | Description |
|----------|-------------|
| [Configuration Guide](./docs/configuration.md) | Environment setup & parameters |
| [Operations Manual](./docs/operations.md) | Deployment, monitoring & troubleshooting |
| [Contributing Guide](./CONTRIBUTING.md) | Development standards & submission process |
| [Yjs Microservice](./docs/solution-2-nodejs-microservice.md) | CRDT architecture design |

---

## 🛠️ Tech Stack

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java">
  <img src="https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white" alt="MySQL">
  <img src="https://img.shields.io/badge/Redis-7-DC382D?style=for-the-badge&logo=redis&logoColor=white" alt="Redis">
  <img src="https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white" alt="Docker">
</p>

---

## 📊 Project Status

- Version: `0.21.0`
- Java: `21`
- Spring Boot: `3.x`
- Status: Active Development

---

## 🤝 Contributing

Issues and Pull Requests are welcome! Please read the [Contributing Guide](./CONTRIBUTING.md) first.

```bash
# Branch naming convention
feature/your-feature
fix/bug-description
docs/documentation-update
```

---

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

<p align="center">
  Made with ❤️ by YUNKE Team
</p>
