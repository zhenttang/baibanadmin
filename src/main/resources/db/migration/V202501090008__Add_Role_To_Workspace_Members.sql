-- 添加 role 字段到 workspace_members 表
-- 用于标识工作空间成员的角色（Owner, Admin, Member, Guest等）

ALTER TABLE `workspace_members` 
ADD COLUMN `role` VARCHAR(50) NULL DEFAULT 'Member' COMMENT '成员角色：Owner, Admin, Member, Guest' 
AFTER `userId`;

-- 为现有数据设置默认角色
-- 如果已有数据，可以根据业务逻辑设置合适的默认值
UPDATE `workspace_members` SET `role` = 'Member' WHERE `role` IS NULL;

