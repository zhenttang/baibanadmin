#!/bin/bash
# 依赖关系分析脚本
# 用于分析Java文件之间的依赖关系，帮助重构

BASE_DIR="/mnt/d/Documents/yunkebaiban/baibanhouduan/yunke-java-backend/src/main/java/com/yunke/backend"
OUTPUT_FILE="dependency_analysis.txt"

echo "=== 代码依赖关系分析 ===" > "$OUTPUT_FILE"
echo "生成时间: $(date)" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

# 分析各层级的依赖关系
echo "=== 1. Controller层依赖分析 ===" >> "$OUTPUT_FILE"
find "$BASE_DIR/controller" -name "*.java" | while read file; do
    echo "文件: $file" >> "$OUTPUT_FILE"
    grep "^import com.yunke.backend" "$file" | sed 's/import //;s/;$//' | while read imp; do
        echo "  -> $imp" >> "$OUTPUT_FILE"
    done
    echo "" >> "$OUTPUT_FILE"
done | head -100

echo "=== 2. Service层依赖分析 ===" >> "$OUTPUT_FILE"
find "$BASE_DIR/service" -name "*.java" | while read file; do
    echo "文件: $file" >> "$OUTPUT_FILE"
    grep "^import com.yunke.backend" "$file" | sed 's/import //;s/;$//' | while read imp; do
        echo "  -> $imp" >> "$OUTPUT_FILE"
    done
    echo "" >> "$OUTPUT_FILE"
done | head -100

echo "=== 3. Entity统计 ===" >> "$OUTPUT_FILE"
echo "Entity文件总数: $(find "$BASE_DIR/entity" -name "*.java" | wc -l)" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "=== 4. Repository统计 ===" >> "$OUTPUT_FILE"
echo "Repository文件总数: $(find "$BASE_DIR/repository" -name "*.java" | wc -l)" >> "$OUTPUT_FILE"
echo "" >> "$OUTPUT_FILE"

echo "分析完成！结果保存在: $OUTPUT_FILE"

