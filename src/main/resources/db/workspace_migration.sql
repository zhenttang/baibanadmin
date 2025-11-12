-- 工作空间表结构修正脚本
-- 修正Workspace实体类与数据库表结构不一致的问题
-- 执行日期: 2025-07-23
-- 说明: 修正sid字段类型，确保与JPA实体类定义一致

-- 1. 备份当前数据（建议在执行前手动备份）
-- mysqldump affine_db workspaces > workspaces_backup_20250723.sql

-- 2. 修正sid字段类型和属性
-- 当前: sid varchar(255) NULL
-- 目标: sid int NOT NULL AUTO_INCREMENT UNIQUE
ALTER TABLE `workspaces` 
DROP COLUMN `sid`;

-- 重新添加sid字段作为自增整数字段
ALTER TABLE `workspaces` 
ADD COLUMN `sid` int NOT NULL AUTO_INCREMENT UNIQUE COMMENT '自增序列号' FIRST,
ADD INDEX `idx_workspaces_sid` (`sid`);

-- 3. 验证表结构是否正确
-- 预期结果: sid字段应为 int NOT NULL AUTO_INCREMENT
-- DESCRIBE `workspaces`;

-- 4. 检查现有数据是否正常
-- SELECT id, sid, name, created_at FROM `workspaces` ORDER BY sid;

-- 注意事项:
-- 1. 执行此脚本前请确保已备份数据
-- 2. sid字段将自动为现有记录分配递增的整数值
-- 3. 新插入的记录将自动获得递增的sid值
-- 4. sid字段不能为空，且具有唯一性约束