#!/bin/bash

# 角色权限系统测试脚本
# 用法: ./test-role-system.sh <TOKEN>

echo "======================================="
echo "🧪 角色权限系统测试脚本"
echo "======================================="

if [ -z "$1" ]; then
    echo "❌ 错误：请提供JWT Token"
    echo "用法: ./test-role-system.sh <YOUR_TOKEN>"
    exit 1
fi

TOKEN=$1
BASE_URL="http://localhost:3010"

echo ""
echo "📋 测试1: 获取可用角色列表"
echo "---------------------------------------"
curl -s -X GET "${BASE_URL}/api/admin/roles/available" \
  -H "Authorization: Bearer ${TOKEN}" | jq '.'

echo ""
echo "📋 测试2: 获取所有管理员"
echo "---------------------------------------"
curl -s -X GET "${BASE_URL}/api/admin/roles/admins" \
  -H "Authorization: Bearer ${TOKEN}" | jq '.'

echo ""
echo "📋 测试3: 测试admin接口访问权限"
echo "---------------------------------------"
echo "访问安全统计接口..."
curl -s -X GET "${BASE_URL}/api/admin/security/stats" \
  -H "Authorization: Bearer ${TOKEN}" | jq '.'

echo ""
echo "======================================="
echo "✅ 测试完成！"
echo "======================================="
echo ""
echo "💡 如果所有接口都返回数据，说明权限正常"
echo "💡 如果返回403，说明您的账号没有管理员权限"
echo ""
echo "🔧 解决方法："
echo "   1. 确保您使用 admin@example.com 登录"
echo "   2. 或者手动执行SQL为您的账号分配ADMIN角色："
echo "      INSERT INTO user_roles (id, user_id, role, enabled) "
echo "      VALUES (UUID(), 'your-user-id', 'ADMIN', TRUE);"
echo ""

