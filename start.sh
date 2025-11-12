#!/bin/bash

# AFFiNE服务快速启动脚本

set -e

echo "🚀 ========================================"
echo "🚀  AFFiNE + YJS微服务 快速启动"
echo "🚀 ========================================"
echo ""

# 检查Docker
if ! command -v docker &> /dev/null; then
    echo "❌ Docker未安装，请先安装Docker"
    exit 1
fi

if ! command -v docker-compose &> /dev/null; then
    echo "❌ Docker Compose未安装，请先安装Docker Compose"
    exit 1
fi

# 步骤1: 安装YJS服务依赖
echo "📦 步骤1: 安装YJS微服务依赖..."
cd yjs-service
if [ ! -d "node_modules" ]; then
    npm install
    echo "✅ YJS服务依赖安装完成"
else
    echo "✅ YJS服务依赖已存在，跳过安装"
fi
cd ..
echo ""

# 步骤2: 构建并启动服务
echo "🔨 步骤2: 构建并启动所有服务..."
docker-compose up -d --build
echo ""

# 步骤3: 等待服务就绪
echo "⏳ 步骤3: 等待服务启动..."
echo "   - 等待YJS微服务..."
until curl -sf http://localhost:3001/health > /dev/null 2>&1; do
    echo -n "."
    sleep 2
done
echo " ✅"

echo "   - 等待Java后端..."
until curl -sf http://localhost:8080/actuator/health > /dev/null 2>&1; do
    echo -n "."
    sleep 3
done
echo " ✅"

echo ""
echo "🎉 ========================================"
echo "🎉  所有服务已成功启动！"
echo "🎉 ========================================"
echo ""
echo "📊 服务信息:"
echo "   • YJS微服务:    http://localhost:3001"
echo "   • YJS健康检查:  http://localhost:3001/health"
echo "   • Java后端:     http://localhost:8080"
echo "   • Socket.IO:    ws://localhost:9092"
echo "   • MySQL:        localhost:3306"
echo "   • Redis:        localhost:6379"
echo ""
echo "📝 常用命令:"
echo "   • 查看日志:     docker-compose logs -f"
echo "   • 停止服务:     docker-compose down"
echo "   • 重启服务:     docker-compose restart"
echo "   • 查看状态:     docker-compose ps"
echo ""
echo "🔍 验证YJS服务:"
echo "   curl http://localhost:3001/health"
echo ""
