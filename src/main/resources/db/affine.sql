/*
 Navicat Premium Dump SQL

 Source Server         : 123456
 Source Server Type    : MySQL
 Source Server Version : 50734 (5.7.34-log)
 Source Host           : localhost:3306
 Source Schema         : affine

 Target Server Type    : MySQL
 Target Server Version : 50734 (5.7.34-log)
 File Encoding         : 65001

 Date: 21/07/2025 16:35:39
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for _data_migrations
-- ----------------------------
DROP TABLE IF EXISTS `_data_migrations`;
CREATE TABLE `_data_migrations`  (
  `finished_at` datetime(6) NULL DEFAULT NULL,
  `started_at` datetime(6) NOT NULL,
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_data_migration_name`(`name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of _data_migrations
-- ----------------------------

-- ----------------------------
-- Table structure for ai_context_embeddings
-- ----------------------------
DROP TABLE IF EXISTS `ai_context_embeddings`;
CREATE TABLE `ai_context_embeddings`  (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键，UUID',
  `context_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '上下文ID，外键关联ai_contexts',
  `embedding` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '嵌入向量，JSON格式存储',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '内容',
  `metadata` json NULL COMMENT '元数据',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_context_embedding_context_id`(`context_id`) USING BTREE,
  CONSTRAINT `fk_ai_context_embedding_context` FOREIGN KEY (`context_id`) REFERENCES `ai_contexts` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI上下文嵌入表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_context_embeddings
-- ----------------------------

-- ----------------------------
-- Table structure for ai_contexts
-- ----------------------------
DROP TABLE IF EXISTS `ai_contexts`;
CREATE TABLE `ai_contexts`  (
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `session_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `config` json NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKl48rl8hdry11huijbo4k2wqs`(`session_id`) USING BTREE,
  CONSTRAINT `FKl48rl8hdry11huijbo4k2wqs` FOREIGN KEY (`session_id`) REFERENCES `ai_sessions_metadata` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_contexts
-- ----------------------------

-- ----------------------------
-- Table structure for ai_jobs
-- ----------------------------
DROP TABLE IF EXISTS `ai_jobs`;
CREATE TABLE `ai_jobs`  (
  `claimed_at` datetime(6) NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `finished_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `error` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `worker_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `workspace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `input` json NULL,
  `output` json NULL,
  `status` enum('PENDING','RUNNING','FINISHED','CLAIMED','FAILED') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `type` enum('TRANSCRIPTION') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKenjv3hj7kdi81t08k1ki6ctfr`(`user_id`) USING BTREE,
  INDEX `FKteu6m781sgoq15igqqn17ovqx`(`workspace_id`) USING BTREE,
  CONSTRAINT `FKenjv3hj7kdi81t08k1ki6ctfr` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKteu6m781sgoq15igqqn17ovqx` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_jobs
-- ----------------------------

-- ----------------------------
-- Table structure for ai_prompt_messages
-- ----------------------------
DROP TABLE IF EXISTS `ai_prompt_messages`;
CREATE TABLE `ai_prompt_messages`  (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `prompt_id` int(11) NOT NULL,
  `idx` int(11) NOT NULL,
  `role` enum('SYSTEM','ASSISTANT','USER') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `attachments` json NULL,
  `params` json NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_prompt_message_idx`(`prompt_id`, `idx`) USING BTREE,
  CONSTRAINT `ai_prompt_messages_ibfk_1` FOREIGN KEY (`prompt_id`) REFERENCES `ai_prompts_metadata` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_prompt_messages
-- ----------------------------

-- ----------------------------
-- Table structure for ai_prompts_messages
-- ----------------------------
DROP TABLE IF EXISTS `ai_prompts_messages`;
CREATE TABLE `ai_prompts_messages`  (
  `idx` int(11) NOT NULL,
  `prompt_id` int(11) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `attachments` json NULL,
  `params` json NULL,
  `role` enum('SYSTEM','ASSISTANT','USER') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_prompt_message_idx`(`prompt_id`, `idx`) USING BTREE,
  CONSTRAINT `FK6fp1nchcshettlnpiib2bdcho` FOREIGN KEY (`prompt_id`) REFERENCES `ai_prompts_metadata` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_prompts_messages
-- ----------------------------

-- ----------------------------
-- Table structure for ai_prompts_metadata
-- ----------------------------
DROP TABLE IF EXISTS `ai_prompts_metadata`;
CREATE TABLE `ai_prompts_metadata`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `action` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `model` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `optional_models` json NULL,
  `config` json NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  `modified` tinyint(1) NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name`) USING BTREE,
  UNIQUE INDEX `UK_86b6jponv8g7239w1proplsso`(`name`) USING BTREE,
  UNIQUE INDEX `uk_ai_prompt_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 4 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_prompts_metadata
-- ----------------------------
INSERT INTO `ai_prompts_metadata` VALUES (1, 'chat', NULL, 'gpt-3.5-turbo', '[\"gpt-4\", \"gpt-3.5-turbo-16k\"]', '{\"max_tokens\": 2000, \"temperature\": 0.7}', '2025-07-11 13:35:09.845', '2025-07-11 13:35:09.851', 0);
INSERT INTO `ai_prompts_metadata` VALUES (2, 'summary', NULL, 'gpt-3.5-turbo', '[\"gpt-4\", \"claude-3-sonnet\"]', '{\"max_tokens\": 500, \"temperature\": 0.3}', '2025-07-11 13:35:09.845', '2025-07-11 13:35:09.856', 0);
INSERT INTO `ai_prompts_metadata` VALUES (3, 'translation', NULL, 'gpt-3.5-turbo', '[\"gpt-4\", \"claude-3-haiku\"]', '{\"max_tokens\": 1000, \"temperature\": 0.1}', '2025-07-11 13:35:09.845', '2025-07-11 13:35:09.861', 0);

-- ----------------------------
-- Table structure for ai_session_messages
-- ----------------------------
DROP TABLE IF EXISTS `ai_session_messages`;
CREATE TABLE `ai_session_messages`  (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `session_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `role` enum('SYSTEM','ASSISTANT','USER') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `attachments` json NULL,
  `params` json NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_session_messages_session_id`(`session_id`) USING BTREE,
  CONSTRAINT `ai_session_messages_ibfk_1` FOREIGN KEY (`session_id`) REFERENCES `ai_sessions` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_session_messages
-- ----------------------------

-- ----------------------------
-- Table structure for ai_sessions
-- ----------------------------
DROP TABLE IF EXISTS `ai_sessions`;
CREATE TABLE `ai_sessions`  (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `workspace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `doc_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `prompt_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `parent_session_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `message_cost` int(11) NOT NULL DEFAULT 0,
  `token_cost` int(11) NOT NULL DEFAULT 0,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `deleted_at` datetime(3) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `prompt_name`(`prompt_name`) USING BTREE,
  INDEX `idx_ai_sessions_user_id`(`user_id`) USING BTREE,
  INDEX `idx_ai_sessions_workspace_user`(`user_id`, `workspace_id`) USING BTREE,
  CONSTRAINT `ai_sessions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `ai_sessions_ibfk_2` FOREIGN KEY (`prompt_name`) REFERENCES `ai_prompts_metadata` (`name`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_sessions
-- ----------------------------

-- ----------------------------
-- Table structure for ai_sessions_messages
-- ----------------------------
DROP TABLE IF EXISTS `ai_sessions_messages`;
CREATE TABLE `ai_sessions_messages`  (
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `session_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `attachments` json NULL,
  `params` json NULL,
  `role` enum('SYSTEM','ASSISTANT','USER') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_session_message_session_id`(`session_id`) USING BTREE,
  CONSTRAINT `FKotwy1t8ap0lthv94rhpuhxacr` FOREIGN KEY (`session_id`) REFERENCES `ai_sessions_metadata` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_sessions_messages
-- ----------------------------

-- ----------------------------
-- Table structure for ai_sessions_metadata
-- ----------------------------
DROP TABLE IF EXISTS `ai_sessions_metadata`;
CREATE TABLE `ai_sessions_metadata`  (
  `message_cost` int(11) NOT NULL,
  `token_cost` int(11) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `deleted_at` datetime(6) NULL DEFAULT NULL,
  `prompt_name` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `doc_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `parent_session_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `workspace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_session_user_id`(`user_id`) USING BTREE,
  INDEX `idx_ai_session_user_workspace`(`user_id`, `workspace_id`) USING BTREE,
  INDEX `FKia1au4e8ldb50ewh6sr5nuemu`(`prompt_name`) USING BTREE,
  CONSTRAINT `FK6ve0aglsgur7drg00l616jacd` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKia1au4e8ldb50ewh6sr5nuemu` FOREIGN KEY (`prompt_name`) REFERENCES `ai_prompts_metadata` (`name`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_sessions_metadata
-- ----------------------------

-- ----------------------------
-- Table structure for ai_workspace_embeddings
-- ----------------------------
DROP TABLE IF EXISTS `ai_workspace_embeddings`;
CREATE TABLE `ai_workspace_embeddings`  (
  `workspace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工作空间ID',
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'GUID',
  `chunk_index` int(11) NOT NULL COMMENT '分块索引',
  `embedding` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '嵌入向量，JSON格式存储',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '内容',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (`workspace_id`, `id`, `chunk_index`) USING BTREE,
  INDEX `idx_ai_workspace_embedding_workspace`(`workspace_id`) USING BTREE,
  CONSTRAINT `fk_ai_workspace_embedding_workspace` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI工作空间嵌入表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_workspace_embeddings
-- ----------------------------

-- ----------------------------
-- Table structure for ai_workspace_file_embedding
-- ----------------------------
DROP TABLE IF EXISTS `ai_workspace_file_embedding`;
CREATE TABLE `ai_workspace_file_embedding`  (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `file_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `embedding` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `chunk_index` int(11) NOT NULL,
  `chunk_text` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_file_id`(`file_id`) USING BTREE,
  CONSTRAINT `fk_embedding_file` FOREIGN KEY (`file_id`) REFERENCES `ai_workspace_files` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of ai_workspace_file_embedding
-- ----------------------------

-- ----------------------------
-- Table structure for ai_workspace_file_embeddings
-- ----------------------------
DROP TABLE IF EXISTS `ai_workspace_file_embeddings`;
CREATE TABLE `ai_workspace_file_embeddings`  (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键，UUID',
  `file_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件ID，外键关联ai_workspace_files',
  `embedding` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '嵌入向量，JSON格式存储',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '内容',
  `metadata` json NULL COMMENT '元数据',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_ai_workspace_file_embedding_file_id`(`file_id`) USING BTREE,
  CONSTRAINT `fk_ai_workspace_file_embedding_file` FOREIGN KEY (`file_id`) REFERENCES `ai_workspace_files` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = 'AI工作空间文件嵌入表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_workspace_file_embeddings
-- ----------------------------

-- ----------------------------
-- Table structure for ai_workspace_files
-- ----------------------------
DROP TABLE IF EXISTS `ai_workspace_files`;
CREATE TABLE `ai_workspace_files`  (
  `size` int(11) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `workspace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `content` mediumblob NOT NULL,
  `metadata` json NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FK3s8plecwta638wjgj6s9xfwc9`(`user_id`) USING BTREE,
  INDEX `FKh9ct0ktsg7to6o2c89ws5a8tw`(`workspace_id`) USING BTREE,
  CONSTRAINT `FK3s8plecwta638wjgj6s9xfwc9` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKh9ct0ktsg7to6o2c89ws5a8tw` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_workspace_files
-- ----------------------------

-- ----------------------------
-- Table structure for ai_workspace_ignored_docs
-- ----------------------------
DROP TABLE IF EXISTS `ai_workspace_ignored_docs`;
CREATE TABLE `ai_workspace_ignored_docs`  (
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `doc_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `workspace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`doc_id`, `workspace_id`) USING BTREE,
  INDEX `FKhgbthltxw4khdpo5cuulinsca`(`workspace_id`) USING BTREE,
  CONSTRAINT `FKhgbthltxw4khdpo5cuulinsca` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of ai_workspace_ignored_docs
-- ----------------------------

-- ----------------------------
-- Table structure for app_configs
-- ----------------------------
DROP TABLE IF EXISTS `app_configs`;
CREATE TABLE `app_configs`  (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置ID，主键',
  `value` json NULL COMMENT '配置值，JSONB类型',
  `last_updated_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后更新者',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_app_config_last_updated_by`(`last_updated_by`) USING BTREE,
  CONSTRAINT `fk_app_config_last_updated_by` FOREIGN KEY (`last_updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '应用配置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of app_configs
-- ----------------------------

-- ----------------------------
-- Table structure for blobs
-- ----------------------------
DROP TABLE IF EXISTS `blobs`;
CREATE TABLE `blobs`  (
  `workspace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `checksum_crc32` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `content_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime NULL DEFAULT NULL,
  `deleted_at` datetime NULL DEFAULT NULL,
  `doc_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `mime` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `size` bigint(20) NOT NULL,
  `storage_path` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `version` int(11) NULL DEFAULT NULL,
  PRIMARY KEY (`workspace_id`, `key`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of blobs
-- ----------------------------
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', '3OujPx_YOY1MTqmgrbWaNDJlJeoLNvTWw96gW22rxps=', '0', 'image/svg+xml', '2025-07-20 13:13:21', NULL, NULL, 'image/svg+xml', 4695, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/3OujPx_YOY1MTqmgrbWaNDJlJeoLNvTWw96gW22rxps=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', '4Pd3nlOWl6vwhEOB6c2Isyhp-O5zALhun7-hKzwanYU=', '0', 'image/png', '2025-07-20 13:13:21', NULL, NULL, 'image/png', 198620, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/4Pd3nlOWl6vwhEOB6c2Isyhp-O5zALhun7-hKzwanYU=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', '74nlTqf1U4wUPx03OANS96AsCk6ZhCGua2n911phsqE=', '0', 'image/png', '2025-07-20 13:13:21', NULL, NULL, 'image/png', 20760, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/74nlTqf1U4wUPx03OANS96AsCk6ZhCGua2n911phsqE=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'cQnt7T9qxI5-It-reeo3E4XVA3HA89L2myi1k2EJfn8=', '0', 'image/svg+xml', '2025-07-20 13:13:06', NULL, NULL, 'image/svg+xml', 1582, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/cQnt7T9qxI5-It-reeo3E4XVA3HA89L2myi1k2EJfn8=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'DZCQ5HoYfKNMdXmV-InmqflwfVWzJ0Eol4ayoEGz0cA=', '0', 'image/png', '2025-07-20 13:13:21', NULL, NULL, 'image/png', 46214, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/DZCQ5HoYfKNMdXmV-InmqflwfVWzJ0Eol4ayoEGz0cA=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'EB0Wx5RCOVW4NnebxvMUoGQuHYFVfmLpspbwTj1xOOQ=', '0', 'image/png', '2025-07-20 13:13:21', NULL, NULL, 'image/png', 70164, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/EB0Wx5RCOVW4NnebxvMUoGQuHYFVfmLpspbwTj1xOOQ=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'ei6CYQwukVrYOfBT6Wd_pWkiSqGLsmg38_DUTdNVXhk=', '0', 'image/svg+xml', '2025-07-20 13:16:36', NULL, NULL, 'image/svg+xml', 203877, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/ei6CYQwukVrYOfBT6Wd_pWkiSqGLsmg38_DUTdNVXhk=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'eT4Nbl90OC9ivTjRBmEabaWqjdmITjCgOtTJNSJu1SU=', '0', 'image/svg+xml', '2025-07-20 13:13:06', NULL, NULL, 'image/svg+xml', 1435, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/eT4Nbl90OC9ivTjRBmEabaWqjdmITjCgOtTJNSJu1SU=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'hC5Z-gYeVnIOEvaGieVzneKIBUfJs2PRxBxLmR6tYf8=', '0', 'image/png', '2025-07-20 13:13:06', NULL, NULL, 'image/png', 22416, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/hC5Z-gYeVnIOEvaGieVzneKIBUfJs2PRxBxLmR6tYf8=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'HDozRCXEtlDfNFFs3sSozkvXUVAP3XXd3zQVI8aW1ak=', '0', 'image/svg+xml', '2025-07-20 13:13:22', NULL, NULL, 'image/svg+xml', 1478, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/HDozRCXEtlDfNFFs3sSozkvXUVAP3XXd3zQVI8aW1ak=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'i3piAMnoD4STQnEjTrAe_ZRdwHcD34n-sJZY8IN1blg=', '0', 'image/svg+xml', '2025-07-20 13:13:06', NULL, NULL, 'image/svg+xml', 7379, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/i3piAMnoD4STQnEjTrAe_ZRdwHcD34n-sJZY8IN1blg=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'IS6xbnAo5WXDRxnP98UBkdOP2Zt2luQXEojcLfnfsR4=', '0', 'image/svg+xml', '2025-07-20 13:13:22', NULL, NULL, 'image/svg+xml', 2375, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/IS6xbnAo5WXDRxnP98UBkdOP2Zt2luQXEojcLfnfsR4=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'j13ZqHGUnVdGW3_1uWw_sFYeHj1SFoNsi5JwrTvpC-k=', '0', 'image/svg+xml', '2025-07-20 13:13:06', NULL, NULL, 'image/svg+xml', 1892, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/j13ZqHGUnVdGW3_1uWw_sFYeHj1SFoNsi5JwrTvpC-k=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'JHrcbru2ztXmKH4JUuYL5ws7uQEvyfhtewbtRiTJY0I=', '0', 'image/png', '2025-07-20 13:13:22', NULL, NULL, 'image/png', 100732, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/JHrcbru2ztXmKH4JUuYL5ws7uQEvyfhtewbtRiTJY0I=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'kwKlgzVYNRk4AyOJs3Xtyt0vMWovo-7BfEqaWndDInM=', '0', 'image/svg+xml', '2025-07-20 13:13:06', NULL, NULL, 'image/svg+xml', 1322, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/kwKlgzVYNRk4AyOJs3Xtyt0vMWovo-7BfEqaWndDInM=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'Qc7GmuDZmGIxbQkYlKi-rA1lcn7-ZbLTzbim0Ww_Oaw=', '0', 'image/png', '2025-07-20 13:13:22', NULL, NULL, 'image/png', 279705, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/Qc7GmuDZmGIxbQkYlKi-rA1lcn7-ZbLTzbim0Ww_Oaw=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'uBJ0gwSEwO5WU8W57ctCiES4y_tVRGPcJwuue4pPbnA=', '0', 'image/png', '2025-07-20 13:13:06', NULL, NULL, 'image/png', 6601, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/uBJ0gwSEwO5WU8W57ctCiES4y_tVRGPcJwuue4pPbnA=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'VZJPB8ZBVtiZ-m04KNtlguY_t9VLx4itHILIQ3l1MRw=', '0', 'image/svg+xml', '2025-07-20 13:13:22', NULL, NULL, 'image/svg+xml', 1600, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/VZJPB8ZBVtiZ-m04KNtlguY_t9VLx4itHILIQ3l1MRw=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'W-RCNTaadPNEI9OALAGHqv1cGmYD1y7KxIRGLsbr-DM=', '0', 'image/svg+xml', '2025-07-20 13:13:05', NULL, NULL, 'image/svg+xml', 1105, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/W-RCNTaadPNEI9OALAGHqv1cGmYD1y7KxIRGLsbr-DM=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', '_dXUvx5tTcm4IykbislTxwNoSLJ4g3oqmd7A9x4ONdY=', '0', 'image/svg+xml', '2025-07-20 13:13:06', NULL, NULL, 'image/svg+xml', 13333, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/_dXUvx5tTcm4IykbislTxwNoSLJ4g3oqmd7A9x4ONdY=', 1);
INSERT INTO `blobs` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', '_T1sv-fm1lUHi646YhoJVfsGfPIorJQSwcO1H5C7JSg=', '0', 'video/mp4', '2025-07-20 13:13:06', NULL, NULL, 'video/mp4', 79106911, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28/_T1sv-fm1lUHi646YhoJVfsGfPIorJQSwcO1H5C7JSg=', 1);

-- ----------------------------
-- Table structure for community_doc_permissions
-- ----------------------------
DROP TABLE IF EXISTS `community_doc_permissions`;
CREATE TABLE `community_doc_permissions`  (
  `id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键，UUID',
  `doc_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文档ID，对应workspace_pages.page_id',
  `user_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '授权用户ID',
  `workspace_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工作空间ID',
  `permission_type` enum('VIEW','COMMENT') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT 'VIEW' COMMENT '权限类型：VIEW-仅查看，COMMENT-可评论（预留）',
  `granted_by` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '授权人ID（文档所有者或管理员）',
  `granted_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '授权时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `unique_doc_user`(`doc_id`, `user_id`) USING BTREE,
  INDEX `idx_doc_id`(`doc_id`) USING BTREE COMMENT '按文档ID查询权限',
  INDEX `idx_user_id`(`user_id`) USING BTREE COMMENT '按用户ID查询可访问的文档',
  INDEX `idx_workspace_id`(`workspace_id`) USING BTREE COMMENT '按工作空间查询自定义权限',
  INDEX `idx_granted_by`(`granted_by`) USING BTREE COMMENT '按授权人查询授权记录',
  CONSTRAINT `community_doc_permissions_ibfk_1` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `community_doc_permissions_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `community_doc_permissions_ibfk_3` FOREIGN KEY (`granted_by`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '社区文档自定义权限表：管理CUSTOM权限类型的具体用户访问权限' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of community_doc_permissions
-- ----------------------------

-- ----------------------------
-- Table structure for community_documents
-- ----------------------------
DROP TABLE IF EXISTS `community_documents`;
CREATE TABLE `community_documents`  (
  `id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文档ID',
  `title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文档标题',
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '文档描述',
  `content_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文档内容URL',
  `author_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '作者ID',
  `category_id` int(11) NULL DEFAULT NULL COMMENT '分类ID',
  `is_paid` tinyint(1) NULL DEFAULT 0 COMMENT '是否付费文档',
  `price` decimal(10, 2) NULL DEFAULT 0.00 COMMENT '文档价格',
  `is_public` tinyint(1) NULL DEFAULT 1 COMMENT '是否公开',
  `require_follow` tinyint(1) NULL DEFAULT 0 COMMENT '是否需要关注才能查看',
  `view_count` int(11) NULL DEFAULT 0 COMMENT '浏览次数',
  `like_count` int(11) NULL DEFAULT 0 COMMENT '点赞次数',
  `collect_count` int(11) NULL DEFAULT 0 COMMENT '收藏次数',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_author_id`(`author_id`) USING BTREE,
  INDEX `idx_category_id`(`category_id`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE,
  INDEX `idx_view_count`(`view_count`) USING BTREE,
  INDEX `idx_like_count`(`like_count`) USING BTREE,
  INDEX `idx_is_paid`(`is_paid`) USING BTREE,
  INDEX `idx_is_public`(`is_public`) USING BTREE,
  FULLTEXT INDEX `idx_title_desc`(`title`, `description`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '社区文档表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of community_documents
-- ----------------------------

-- ----------------------------
-- Table structure for copilot_messages
-- ----------------------------
DROP TABLE IF EXISTS `copilot_messages`;
CREATE TABLE `copilot_messages`  (
  `tokens` int(11) NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `message_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `session_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `finish_reason` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `attachments` json NULL,
  `params` json NULL,
  `role` enum('USER','ASSISTANT','SYSTEM') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`message_id`) USING BTREE,
  INDEX `FKsakat2shmcwnnicn87n90hdgi`(`session_id`) USING BTREE,
  CONSTRAINT `FKsakat2shmcwnnicn87n90hdgi` FOREIGN KEY (`session_id`) REFERENCES `copilot_sessions` (`session_id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of copilot_messages
-- ----------------------------

-- ----------------------------
-- Table structure for copilot_quotas
-- ----------------------------
DROP TABLE IF EXISTS `copilot_quotas`;
CREATE TABLE `copilot_quotas`  (
  `limit_per_day` int(11) NULL DEFAULT NULL,
  `limit_per_month` int(11) NULL DEFAULT NULL,
  `token_limit_per_day` int(11) NULL DEFAULT NULL,
  `token_limit_per_month` int(11) NULL DEFAULT NULL,
  `tokens_used_this_month` int(11) NULL DEFAULT NULL,
  `tokens_used_today` int(11) NULL DEFAULT NULL,
  `used_this_month` int(11) NULL DEFAULT NULL,
  `used_today` int(11) NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `last_reset_date` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `user_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `workspace_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `feature` enum('CHAT','IMAGE_GENERATION','TEXT_GENERATION','CODE_COMPLETION','TRANSLATION','SUMMARIZATION') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UKcl386mjb7059epogc3asawga1`(`user_id`, `feature`) USING BTREE,
  UNIQUE INDEX `UKsay9loxo1ramoo3uns5mcbyh9`(`workspace_id`, `feature`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of copilot_quotas
-- ----------------------------

-- ----------------------------
-- Table structure for copilot_sessions
-- ----------------------------
DROP TABLE IF EXISTS `copilot_sessions`;
CREATE TABLE `copilot_sessions`  (
  `message_count` int(11) NULL DEFAULT NULL,
  `tokens_used` int(11) NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `finished_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NULL DEFAULT NULL,
  `doc_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `session_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `workspace_id` varchar(36) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `model` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `config` json NULL,
  `provider` enum('OPENAI','ANTHROPIC','GOOGLE','AZURE_OPENAI','OLLAMA') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` enum('ACTIVE','FINISHED','CANCELLED','ERROR') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`session_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of copilot_sessions
-- ----------------------------

-- ----------------------------
-- Table structure for data_migrations
-- ----------------------------
DROP TABLE IF EXISTS `data_migrations`;
CREATE TABLE `data_migrations`  (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `migration_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `version_num` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `status` enum('PENDING','RUNNING','COMPLETED','FAILED') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING',
  `started_at` datetime(3) NULL DEFAULT NULL,
  `completed_at` datetime(3) NULL DEFAULT NULL,
  `error_message` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `updated_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `migration_name`(`migration_name`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of data_migrations
-- ----------------------------

-- ----------------------------
-- Table structure for deprecated_app_runtime_settings
-- ----------------------------
DROP TABLE IF EXISTS `deprecated_app_runtime_settings`;
CREATE TABLE `deprecated_app_runtime_settings`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置键',
  `type` enum('SYSTEM','USER','WORKSPACE') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '配置类型',
  `payload` json NULL COMMENT '负载，JSONB类型',
  `last_updated_by_user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '最后更新用户ID',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_deprecated_app_runtime_settings_key`(`key`) USING BTREE,
  INDEX `idx_deprecated_app_runtime_settings_last_updated_by`(`last_updated_by_user_id`) USING BTREE,
  CONSTRAINT `fk_deprecated_app_runtime_settings_last_updated_by` FOREIGN KEY (`last_updated_by_user_id`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '已弃用应用运行时设置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of deprecated_app_runtime_settings
-- ----------------------------

-- ----------------------------
-- Table structure for deprecated_user_invoices
-- ----------------------------
DROP TABLE IF EXISTS `deprecated_user_invoices`;
CREATE TABLE `deprecated_user_invoices`  (
  `amount` int(11) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `stripe_created` datetime(6) NOT NULL,
  `stripe_due_date` datetime(6) NULL DEFAULT NULL,
  `stripe_period_end` datetime(6) NOT NULL,
  `stripe_period_start` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `currency` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `stripe_customer_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `stripe_hosted_invoice_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `stripe_invoice_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `stripe_pdf_invoice_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `stripe_subscription_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`stripe_invoice_id`) USING BTREE,
  INDEX `FK6iuh41ukd82anooieio4iy9rq`(`user_id`) USING BTREE,
  CONSTRAINT `FK6iuh41ukd82anooieio4iy9rq` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of deprecated_user_invoices
-- ----------------------------

-- ----------------------------
-- Table structure for deprecated_user_subscriptions
-- ----------------------------
DROP TABLE IF EXISTS `deprecated_user_subscriptions`;
CREATE TABLE `deprecated_user_subscriptions`  (
  `cancel_at_period_end` bit(1) NOT NULL,
  `cancel_at` datetime(6) NULL DEFAULT NULL,
  `canceled_at` datetime(6) NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `stripe_created` datetime(6) NOT NULL,
  `stripe_current_period_end` datetime(6) NOT NULL,
  `stripe_current_period_start` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `status` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `stripe_customer_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `stripe_price_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `stripe_subscription_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of deprecated_user_subscriptions
-- ----------------------------

-- ----------------------------
-- Table structure for doc_export_formats
-- ----------------------------
DROP TABLE IF EXISTS `doc_export_formats`;
CREATE TABLE `doc_export_formats`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '主键，自增',
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '格式名称',
  `display_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '显示名称',
  `mime_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'MIME类型',
  `extension` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '文件扩展名',
  `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `supported_features` json NULL COMMENT '支持的功能，JSON数组',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_doc_export_format_name`(`name`) USING BTREE,
  INDEX `idx_doc_export_format_enabled`(`enabled`) USING BTREE,
  INDEX `idx_doc_export_format_mime_type`(`mime_type`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '文档导出格式表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of doc_export_formats
-- ----------------------------
INSERT INTO `doc_export_formats` VALUES (1, 'PDF', 'PDF文档', 'application/pdf', '.pdf', 1, '[\"text\", \"images\", \"tables\"]', '2025-07-15 15:23:33.145204', '2025-07-15 15:23:33.145204');
INSERT INTO `doc_export_formats` VALUES (2, 'DOCX', 'Word文档', 'application/vnd.openxmlformats-officedocument.wordprocessingml.document', '.docx', 1, '[\"text\", \"images\", \"tables\", \"styles\"]', '2025-07-15 15:23:33.145204', '2025-07-15 15:23:33.145204');
INSERT INTO `doc_export_formats` VALUES (3, 'HTML', 'HTML网页', 'text/html', '.html', 1, '[\"text\", \"images\", \"tables\", \"styles\", \"links\"]', '2025-07-15 15:23:33.145204', '2025-07-15 15:23:33.145204');
INSERT INTO `doc_export_formats` VALUES (4, 'MARKDOWN', 'Markdown文档', 'text/markdown', '.md', 1, '[\"text\", \"code\", \"tables\", \"links\"]', '2025-07-15 15:23:33.145204', '2025-07-15 15:23:33.145204');
INSERT INTO `doc_export_formats` VALUES (5, 'TXT', '纯文本', 'text/plain', '.txt', 1, '[\"text\"]', '2025-07-15 15:23:33.145204', '2025-07-15 15:23:33.145204');

-- ----------------------------
-- Table structure for document_categories
-- ----------------------------
DROP TABLE IF EXISTS `document_categories`;
CREATE TABLE `document_categories`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '分类名称',
  `description` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类描述',
  `icon` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '分类图标',
  `sort_order` int(11) NULL DEFAULT 0 COMMENT '排序序号',
  `is_active` tinyint(1) NULL DEFAULT 1 COMMENT '是否激活',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name`) USING BTREE,
  INDEX `idx_sort_order`(`sort_order`) USING BTREE,
  INDEX `idx_is_active`(`is_active`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 12 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文档分类表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of document_categories
-- ----------------------------
INSERT INTO `document_categories` VALUES (1, '技术分享', '技术相关的文档和教程', '🔧', 1, 1, '2025-07-21 09:40:03');
INSERT INTO `document_categories` VALUES (2, '设计创意', '界面设计、创意灵感相关内容', '🎨', 2, 1, '2025-07-21 09:40:03');
INSERT INTO `document_categories` VALUES (3, '产品运营', '产品管理、运营策略相关内容', '📊', 3, 1, '2025-07-21 09:40:03');
INSERT INTO `document_categories` VALUES (4, '职场成长', '职业发展、技能提升相关内容', '📈', 4, 1, '2025-07-21 09:40:03');
INSERT INTO `document_categories` VALUES (5, '生活随笔', '生活感悟、个人经历相关内容', '📝', 5, 1, '2025-07-21 09:40:03');
INSERT INTO `document_categories` VALUES (6, '学习笔记', '个人学习和总结', '📚', 5, 1, '2025-07-21 09:40:03');
INSERT INTO `document_categories` VALUES (7, '产品设计', '产品设计和用户体验相关', '🎨', 2, 1, '2025-07-21 09:40:40');
INSERT INTO `document_categories` VALUES (8, '项目管理', '项目管理和团队协作', '📋', 3, 1, '2025-07-21 09:40:40');
INSERT INTO `document_categories` VALUES (9, '市场营销', '市场推广和营销策略', '📊', 4, 1, '2025-07-21 09:40:40');

-- ----------------------------
-- Table structure for document_collections
-- ----------------------------
DROP TABLE IF EXISTS `document_collections`;
CREATE TABLE `document_collections`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `document_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文档ID',
  `user_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户ID',
  `collection_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '默认收藏夹' COMMENT '收藏夹名称',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_doc_user_collect`(`document_id`, `user_id`) USING BTREE,
  INDEX `idx_document_id`(`document_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_collection_name`(`collection_name`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文档收藏表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of document_collections
-- ----------------------------

-- ----------------------------
-- Table structure for document_likes
-- ----------------------------
DROP TABLE IF EXISTS `document_likes`;
CREATE TABLE `document_likes`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '点赞ID',
  `document_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文档ID',
  `user_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户ID',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_doc_user_like`(`document_id`, `user_id`) USING BTREE,
  INDEX `idx_document_id`(`document_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文档点赞表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of document_likes
-- ----------------------------

-- ----------------------------
-- Table structure for document_tag_relations
-- ----------------------------
DROP TABLE IF EXISTS `document_tag_relations`;
CREATE TABLE `document_tag_relations`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '关联ID',
  `document_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文档ID',
  `tag_id` int(11) NOT NULL COMMENT '标签ID',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_doc_tag`(`document_id`, `tag_id`) USING BTREE,
  INDEX `idx_document_id`(`document_id`) USING BTREE,
  INDEX `idx_tag_id`(`tag_id`) USING BTREE,
  CONSTRAINT `document_tag_relations_ibfk_1` FOREIGN KEY (`tag_id`) REFERENCES `document_tags` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文档标签关联表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of document_tag_relations
-- ----------------------------

-- ----------------------------
-- Table structure for document_tags
-- ----------------------------
DROP TABLE IF EXISTS `document_tags`;
CREATE TABLE `document_tags`  (
  `id` int(11) NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '标签名称',
  `color` varchar(7) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '#666666' COMMENT '标签颜色',
  `usage_count` int(11) NULL DEFAULT 0 COMMENT '使用次数',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `name`(`name`) USING BTREE,
  INDEX `idx_usage_count`(`usage_count`) USING BTREE,
  INDEX `idx_name`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 41 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '文档标签表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of document_tags
-- ----------------------------
INSERT INTO `document_tags` VALUES (1, 'React', '#61DAFB', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (2, 'Vue', '#4FC08D', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (3, 'JavaScript', '#F7DF1E', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (4, 'TypeScript', '#3178C6', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (5, 'Node.js', '#339933', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (6, 'Python', '#3776AB', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (7, 'Java', '#ED8B00', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (8, 'MySQL', '#4479A1', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (9, 'Redis', '#DC382D', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (10, 'Docker', '#2496ED', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (11, 'Kubernetes', '#326CE5', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (12, '微服务', '#FF6B81', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (13, '前端', '#4ECDC4', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (14, '后端', '#45B7D1', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (15, '全栈', '#96CEB4', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (16, 'UI设计', '#FFEAA7', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (17, 'UX设计', '#DDA0DD', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (18, '产品设计', '#98D8C8', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (19, '数据分析', '#F7DC6F', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (20, '机器学习', '#5F27CD', 0, '2025-07-21 09:40:03');
INSERT INTO `document_tags` VALUES (21, 'Spring Boot', '#6DB33F', 0, '2025-07-21 09:40:40');
INSERT INTO `document_tags` VALUES (22, '前端开发', '#FF6B6B', 0, '2025-07-21 09:40:40');
INSERT INTO `document_tags` VALUES (23, '后端开发', '#4ECDC4', 0, '2025-07-21 09:40:40');
INSERT INTO `document_tags` VALUES (24, '全栈开发', '#45B7D1', 0, '2025-07-21 09:40:40');
INSERT INTO `document_tags` VALUES (25, '移动开发', '#96CEB4', 0, '2025-07-21 09:40:40');
INSERT INTO `document_tags` VALUES (26, 'DevOps', '#FF9F43', 0, '2025-07-21 09:40:40');
INSERT INTO `document_tags` VALUES (27, '云计算', '#54A0FF', 0, '2025-07-21 09:40:40');
INSERT INTO `document_tags` VALUES (28, '区块链', '#FFD700', 0, '2025-07-21 09:40:40');
INSERT INTO `document_tags` VALUES (29, 'API设计', '#26DE81', 0, '2025-07-21 09:40:40');
INSERT INTO `document_tags` VALUES (30, '数据库设计', '#4834D4', 0, '2025-07-21 09:40:40');
INSERT INTO `document_tags` VALUES (31, '性能优化', '#FF9FF3', 0, '2025-07-21 09:40:40');

-- ----------------------------
-- Table structure for features
-- ----------------------------
DROP TABLE IF EXISTS `features`;
CREATE TABLE `features`  (
  `deprecated_type` int(11) NOT NULL,
  `deprecated_version` int(11) NOT NULL,
  `enabled` bit(1) NOT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` int(11) NOT NULL,
  `version` int(11) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `description` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '功能名称',
  `configs` json NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_feature_name_version`(`name`, `version`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of features
-- ----------------------------

-- ----------------------------
-- Table structure for installed_licenses
-- ----------------------------
DROP TABLE IF EXISTS `installed_licenses`;
CREATE TABLE `installed_licenses`  (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键，UUID',
  `license_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '许可证ID',
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '用户ID',
  `workspace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '工作空间ID',
  `installed_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '安装时间',
  `activated_at` datetime(6) NULL DEFAULT NULL COMMENT '激活时间',
  `deactivated_at` datetime(6) NULL DEFAULT NULL COMMENT '停用时间',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_installed_license_license_id`(`license_id`) USING BTREE,
  INDEX `idx_installed_license_user_id`(`user_id`) USING BTREE,
  INDEX `idx_installed_license_workspace_id`(`workspace_id`) USING BTREE,
  CONSTRAINT `fk_installed_license_license` FOREIGN KEY (`license_id`) REFERENCES `licenses` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_installed_license_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `fk_installed_license_workspace` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '已安装许可证表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of installed_licenses
-- ----------------------------

-- ----------------------------
-- Table structure for invoices
-- ----------------------------
DROP TABLE IF EXISTS `invoices`;
CREATE TABLE `invoices`  (
  `amount` decimal(10, 2) NOT NULL,
  `currency` varchar(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `onetime_subscription_redeemed` bit(1) NOT NULL,
  `subscription_id` int(11) NULL DEFAULT NULL,
  `subtotal` decimal(10, 2) NULL DEFAULT NULL,
  `tax` decimal(10, 2) NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `due_date` datetime(6) NULL DEFAULT NULL,
  `paid_at` datetime(6) NULL DEFAULT NULL,
  `period_end` datetime(6) NULL DEFAULT NULL,
  `period_start` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `last_payment_error` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `link` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `stripe_invoice_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `target_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `workspace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `metadata` json NULL,
  `status` enum('DRAFT','OPEN','PAID','UNCOLLECTIBLE','VOID') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`stripe_invoice_id`) USING BTREE,
  INDEX `idx_invoice_target_id`(`target_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of invoices
-- ----------------------------

-- ----------------------------
-- Table structure for licenses
-- ----------------------------
DROP TABLE IF EXISTS `licenses`;
CREATE TABLE `licenses`  (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键，UUID',
  `key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '许可证密钥',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型',
  `plan` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '计划',
  `quantity` int(11) NOT NULL DEFAULT 1 COMMENT '数量',
  `expired_at` datetime(6) NULL DEFAULT NULL COMMENT '过期时间',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_license_key`(`key`) USING BTREE,
  INDEX `idx_license_type`(`type`) USING BTREE,
  INDEX `idx_license_plan`(`plan`) USING BTREE,
  INDEX `idx_license_expired_at`(`expired_at`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '许可证表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of licenses
-- ----------------------------

-- ----------------------------
-- Table structure for mail_queue
-- ----------------------------
DROP TABLE IF EXISTS `mail_queue`;
CREATE TABLE `mail_queue`  (
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `max_retries` int(11) NOT NULL,
  `retry_count` int(11) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `next_retry_at` datetime(6) NULL DEFAULT NULL,
  `sent_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `error_message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `error_stack` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `mail_type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `parameters` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `recipient_email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `recipient_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `subject` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `priority` enum('LOW','NORMAL','HIGH','URGENT') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` enum('PENDING','PROCESSING','SENT','FAILED','CANCELLED') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mail_queue
-- ----------------------------

-- ----------------------------
-- Table structure for mail_templates
-- ----------------------------
DROP TABLE IF EXISTS `mail_templates`;
CREATE TABLE `mail_templates`  (
  `enabled` bit(1) NOT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `version` int(11) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `created_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `default_variables` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `description` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `display_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `html_template` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `language` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `subject_template` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `text_template` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `category` enum('USER','WORKSPACE','TEAM','DOCUMENT','SYSTEM','NOTIFICATION') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_6rqktido2sab0mlrtrw6xatgn`(`name`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of mail_templates
-- ----------------------------

-- ----------------------------
-- Table structure for multiple_users_sessions
-- ----------------------------
DROP TABLE IF EXISTS `multiple_users_sessions`;
CREATE TABLE `multiple_users_sessions`  (
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NULL DEFAULT NULL,
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of multiple_users_sessions
-- ----------------------------
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-15 04:24:34.837771', '2025-07-16 04:24:34.747058', '05cd336a-9e29-4ca5-8249-b83ca9b3f57c');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-16 02:04:35.196611', '2025-07-17 02:04:35.194611', '07a93cc5-d627-4b45-a6bd-a507e2b68d5a');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-17 00:01:30.716753', '2025-07-18 00:01:30.712515', '0b6671b3-d4b0-4634-9286-6b795dad404e');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-15 14:04:38.780783', '2025-07-16 14:04:38.778260', '0ebec862-9752-4817-bf47-bd823f273000');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-15 14:04:38.593938', '2025-07-16 14:04:38.591953', '0f3dd133-3167-46f6-88ca-58e3a4b96ec7');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-17 00:01:30.914268', '2025-07-18 00:01:30.910420', '1805d6ee-e8a3-4946-b4c0-4b11be58e871');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-15 14:04:38.443563', '2025-07-16 14:04:38.432370', '1a6056f4-2266-47fc-be43-ef37561b27cf');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-16 00:43:17.235417', '2025-07-17 00:43:17.217906', '1d2d76a7-75f1-43f4-a8a9-dbff4e263c80');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-17 00:00:54.371499', '2025-07-18 00:00:54.220432', '1e28558d-cd39-4327-871c-aad02e39ba4a');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-15 14:04:37.994883', '2025-07-16 14:04:37.990641', '2731dd62-4dc9-4b3e-8930-e3b5dccb01f6');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-17 00:01:30.507970', '2025-07-18 00:01:30.504168', '2cae7257-5bbe-48d9-8afe-49ea88388be5');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-17 00:01:32.037023', '2025-07-18 00:01:32.029966', '2d1b97c3-efe6-40d6-a14d-28d812bb19bb');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-16 01:51:05.757309', '2025-07-17 01:51:05.754775', '2f56bc9a-07f1-4db8-8b4f-df8f6b32cf55');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-16 01:40:09.927497', '2025-07-17 01:40:09.851955', '38900528-671a-4cd2-b152-f03bedf76c4f');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-17 00:01:31.291051', '2025-07-18 00:01:31.287047', '3c3bace7-ae31-4492-864d-46b7bbaefd26');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-17 00:01:29.583381', '2025-07-18 00:01:29.580169', '3fe779ce-6261-43eb-b915-2198a86c1eee');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-17 00:01:08.543983', '2025-07-18 00:01:08.539659', '492f4d10-1c4a-495d-bea4-7aed85a0e665');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-17 00:01:30.338253', '2025-07-18 00:01:30.331903', '4cdbf785-47fe-4eb4-870b-d1597590fcd1');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-16 02:21:06.853297', '2025-07-17 02:21:06.852296', '52f08692-fb1c-4db2-a841-7e8c08c9bb1c');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-17 00:01:31.479160', '2025-07-18 00:01:31.472675', '58fc08aa-dc85-4b28-8a87-ee08d2720dae');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-17 02:53:11.478505', '2025-07-18 02:53:11.445087', '595b0335-2f17-4d11-adac-f3736df6e83a');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-17 00:01:31.112303', '2025-07-18 00:01:31.110009', '5d0077e6-a0c0-4456-bc2a-aad0e8da3c88');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-17 00:01:30.145887', '2025-07-18 00:01:30.141975', '5dc5ed79-46e9-4f0a-9ace-a2c28aab84b3');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-15 14:03:59.031566', '2025-07-16 14:03:59.021755', '76018301-7ee9-4428-af84-143306b76956');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-16 02:01:31.261573', '2025-07-17 02:01:31.217333', '7c0531f9-c361-41df-8f39-5935e638288a');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-16 02:48:08.959431', '2025-07-17 02:48:08.956384', '7ef08794-ddec-4343-979b-c5a705acd956');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-16 02:10:50.487735', '2025-07-17 02:10:50.484723', '884559a3-66c8-450e-863d-cc0729f1cc59');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-15 14:04:24.093807', '2025-07-16 14:04:24.085383', '89684009-7774-4fb1-a5df-4b69ed61cea4');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-16 01:13:35.420611', '2025-07-17 01:13:35.409479', '917f5770-fe61-41d9-8b9d-2b759aa7b243');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-15 14:04:38.212496', '2025-07-16 14:04:38.210048', 'a4006221-24a9-408a-a7a0-cfe0a9be0ccc');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-17 00:01:29.770230', '2025-07-18 00:01:29.765195', 'a995dc42-d1c5-4769-b1b7-1d4ebc8ffa92');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-16 01:44:54.492337', '2025-07-17 01:44:54.485957', 'ae07a39a-8327-4d0b-865d-da7e96cb1a17');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-16 01:59:22.463199', '2025-07-17 01:59:22.454657', 'ae5d4023-dc78-4150-afcc-b078c88a0660');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-15 14:03:47.851563', '2025-07-16 14:03:47.770435', 'afb78ed1-de60-482c-979f-5cfc09bce4c8');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-17 00:01:31.667948', '2025-07-18 00:01:31.661174', 'ba76b93d-b99e-447c-9dbe-6f5d10f7c481');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-17 00:01:29.963325', '2025-07-18 00:01:29.958646', 'c2ca66d1-8cb8-4d8b-9a6d-0309dbac1672');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-16 02:32:58.300837', '2025-07-17 02:32:58.297391', 'c688394b-88dd-4a27-8880-753d14065089');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-16 02:15:25.010057', '2025-07-17 02:15:25.008057', 'ce0d5abb-e029-49fb-ae9b-8f62c509d9f0');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-15 14:04:00.314915', '2025-07-16 14:04:00.310792', 'decfacfe-bae8-4201-a2e6-f45f3e174b59');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-16 02:30:34.188969', '2025-07-17 02:30:34.094639', 'e4011025-2182-46f4-81cc-a0fc15c91c17');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-17 00:01:28.651142', '2025-07-18 00:01:28.645771', 'e82d136a-a037-48e6-9b08-6f027e5d94ef');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-15 14:06:19.415466', '2025-07-16 14:06:19.413470', 'f391ef6a-accf-4b0f-b707-5614f3b16609');
INSERT INTO `multiple_users_sessions` VALUES ('2025-07-16 01:41:07.307881', '2025-07-17 01:41:07.304850', 'f3ec5431-39b4-430e-8fbd-8cb0f8c2c0d4');

-- ----------------------------
-- Table structure for notifications
-- ----------------------------
DROP TABLE IF EXISTS `notifications`;
CREATE TABLE `notifications`  (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键，UUID',
  `recipient_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '收件人ID',
  `type` enum('SYSTEM','WORKSPACE','DOCUMENT','FEATURE','PAYMENT') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '通知类型',
  `level` enum('INFO','WARNING','ERROR','SUCCESS') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '通知级别',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '内容',
  `data` json NULL COMMENT '数据，JSON类型',
  `read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已读',
  `read_at` datetime(6) NULL DEFAULT NULL COMMENT '已读时间',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  `expires_at` datetime(6) NULL DEFAULT NULL COMMENT '过期时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_notification_recipient_created`(`recipient_id`, `created_at`) USING BTREE,
  INDEX `idx_notification_type`(`type`) USING BTREE,
  INDEX `idx_notification_read`(`read`) USING BTREE,
  CONSTRAINT `fk_notification_recipient` FOREIGN KEY (`recipient_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '通知表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of notifications
-- ----------------------------

-- ----------------------------
-- Table structure for payment_records
-- ----------------------------
DROP TABLE IF EXISTS `payment_records`;
CREATE TABLE `payment_records`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '记录ID',
  `user_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户ID',
  `document_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '文档ID',
  `amount` decimal(10, 2) NOT NULL COMMENT '支付金额',
  `payment_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '支付方式',
  `transaction_id` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '交易ID',
  `status` enum('PENDING','SUCCESS','FAILED','REFUNDED') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'PENDING' COMMENT '支付状态',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_document`(`user_id`, `document_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_document_id`(`document_id`) USING BTREE,
  INDEX `idx_status`(`status`) USING BTREE,
  INDEX `idx_transaction_id`(`transaction_id`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '支付记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of payment_records
-- ----------------------------

-- ----------------------------
-- Table structure for payments
-- ----------------------------
DROP TABLE IF EXISTS `payments`;
CREATE TABLE `payments`  (
  `amount` decimal(10, 2) NOT NULL,
  `currency` varchar(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `refunded_amount` decimal(10, 2) NULL DEFAULT NULL,
  `subscription_id` int(11) NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `id` bigint(20) NOT NULL AUTO_INCREMENT,
  `processed_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL,
  `failure_code` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `failure_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `invoice_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `stripe_charge_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `stripe_payment_intent_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `target_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `metadata` json NULL,
  `method` enum('CARD','BANK_TRANSFER','ALIPAY','WECHAT_PAY','PAYPAL') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` enum('PENDING','PROCESSING','SUCCEEDED','FAILED','CANCELED','REQUIRES_ACTION','REQUIRES_CONFIRMATION') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_puc8mkpduwb4ws7khxcoo0s3t`(`stripe_payment_intent_id`) USING BTREE,
  INDEX `idx_payment_target_id`(`target_id`) USING BTREE,
  INDEX `idx_payment_stripe_id`(`stripe_payment_intent_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of payments
-- ----------------------------

-- ----------------------------
-- Table structure for quota_usage
-- ----------------------------
DROP TABLE IF EXISTS `quota_usage`;
CREATE TABLE `quota_usage`  (
  `document_count` int(11) NOT NULL,
  `file_count` int(11) NOT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `used_copilot_actions` int(11) NOT NULL,
  `used_members` int(11) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `reset_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `used_history_records` bigint(20) NOT NULL,
  `used_storage` bigint(20) NOT NULL,
  `period` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `target_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `resource_type` enum('STORAGE','MEMBER','COPILOT','HISTORY','FILE','DOCUMENT') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `usage_type` enum('USER','WORKSPACE') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of quota_usage
-- ----------------------------

-- ----------------------------
-- Table structure for search_logs
-- ----------------------------
DROP TABLE IF EXISTS `search_logs`;
CREATE TABLE `search_logs`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '用户ID',
  `search_keyword` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '搜索关键词',
  `search_type` enum('TITLE','CONTENT','AUTHOR','TAG','CATEGORY') CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT 'TITLE' COMMENT '搜索类型',
  `result_count` int(11) NULL DEFAULT 0 COMMENT '结果数量',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id`) USING BTREE,
  INDEX `idx_keyword`(`search_keyword`) USING BTREE,
  INDEX `idx_search_type`(`search_type`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '搜索日志表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of search_logs
-- ----------------------------

-- ----------------------------
-- Table structure for sessions
-- ----------------------------
DROP TABLE IF EXISTS `sessions`;
CREATE TABLE `sessions`  (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '会话ID，主键',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  `expires_at` datetime(6) NULL DEFAULT NULL COMMENT '过期时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_session_expires_at`(`expires_at`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '会话表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of sessions
-- ----------------------------

-- ----------------------------
-- Table structure for snapshot_histories
-- ----------------------------
DROP TABLE IF EXISTS `snapshot_histories`;
CREATE TABLE `snapshot_histories`  (
  `workspace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工作空间ID',
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'GUID',
  `timestamp` bigint(20) NOT NULL COMMENT '时间戳',
  `blob` longblob NOT NULL COMMENT '二进制数据',
  `state` longblob NULL COMMENT '状态数据',
  `expired_at` datetime(6) NULL DEFAULT NULL COMMENT '过期时间',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  PRIMARY KEY (`workspace_id`, `id`, `timestamp`) USING BTREE,
  INDEX `idx_snapshot_history_workspace_id`(`workspace_id`, `id`) USING BTREE,
  CONSTRAINT `fk_snapshot_history_snapshot` FOREIGN KEY (`workspace_id`, `id`) REFERENCES `snapshots` (`workspace_id`, `id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '快照历史表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of snapshot_histories
-- ----------------------------

-- ----------------------------
-- Table structure for snapshots
-- ----------------------------
DROP TABLE IF EXISTS `snapshots`;
CREATE TABLE `snapshots`  (
  `workspace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工作空间ID',
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'GUID',
  `blob` longblob NOT NULL COMMENT '二进制数据',
  `state` longblob NULL COMMENT '状态数据',
  `seq` int(11) NOT NULL DEFAULT 0 COMMENT '序列号',
  `created_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建者',
  `updated_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '更新者',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (`workspace_id`, `id`) USING BTREE,
  INDEX `idx_snapshot_workspace_updated`(`workspace_id`, `updated_at`) USING BTREE,
  INDEX `idx_snapshot_created_by`(`created_by`) USING BTREE,
  INDEX `idx_snapshot_updated_by`(`updated_by`) USING BTREE,
  CONSTRAINT `fk_snapshot_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_snapshot_updated_by` FOREIGN KEY (`updated_by`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_snapshot_workspace` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '快照表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of snapshots
-- ----------------------------
INSERT INTO `snapshots` VALUES ('10ed579e-43a6-4754-a708-5beab282ee95', 'test-doc-1752718947713', 0x0102030405060708090A, 0x5354415445000001981630B665C5D38B660000000A, 1, NULL, NULL, '2025-07-17 02:22:27.940448', '2025-07-17 02:22:27.941445');
INSERT INTO `snapshots` VALUES ('10ed579e-43a6-4754-a708-5beab282ee95', 'test-doc-edit-1752720337161', 0x010203313131313131333333, 0x5354415445000001981645E93AB68594320000000C, 1, NULL, NULL, '2025-07-17 02:45:37.210119', '2025-07-17 02:45:37.210119');
INSERT INTO `snapshots` VALUES ('10ed579e-43a6-4754-a708-5beab282ee95', 'test-doc-edit-1752720349576', 0x010203343434343434343434, 0x535441544500000198164619A14EF34AF30000000C, 1, NULL, NULL, '2025-07-17 02:45:49.601428', '2025-07-17 02:45:49.601428');
INSERT INTO `snapshots` VALUES ('10ed579e-43a6-4754-a708-5beab282ee95', 'test-doc-edit-1752723292953', 0x0102033131313131313131313131, 0x535441544500000198167303B72B5436900000000E, 1, NULL, NULL, '2025-07-17 03:34:53.111127', '2025-07-17 03:34:53.111127');
INSERT INTO `snapshots` VALUES ('8db6eaab-edb5-4b57-9b76-6aa1bca3161c', 'test-doc-edit-1752726832050', 0x010203C2B73131313131, 0x53544154450000019816A903E19FCFED5B0000000A, 1, NULL, NULL, '2025-07-17 04:33:52.097470', '2025-07-17 04:33:52.097470');
INSERT INTO `snapshots` VALUES ('8db6eaab-edb5-4b57-9b76-6aa1bca3161c', 'test-doc-edit-1752726853378', 0x0102033333333333333333333333, 0x53544154450000019816A95719A34EFD520000000E, 1, NULL, NULL, '2025-07-17 04:34:13.401681', '2025-07-17 04:34:13.401681');
INSERT INTO `snapshots` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', '5hEJ4IsCauFwimPr2RcaE', 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203333333333333333333333333333330A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, 0x53544154450000019828558189C08D8E080000020B, 1, NULL, NULL, '2025-07-20 14:55:49.129816', '2025-07-20 14:55:49.129816');
INSERT INTO `snapshots` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'IfIGKlDIgPE3MWqt1tRvG', 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E68891E697A0E6958CE6B58BE8AF950A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E68891E697A0E6958CE6B58BE8AF950A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E68891E697A0E6958CE6B58BE8AF95E5919CE5919CE5919CE5919CE5919CE5919C0A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E68891E697A0E6958CE6B58BE8AF95E5919CE5919CE5919CE5919CE5919CE5919CE5A4AAE783ADE789B9E789B90A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E68891E697A0E6958CE6B58BE8AF95E5919CE5919CE5919CE5919CE5919CE5919CE5A4AAE783ADE789B9E789B9E783ADE783ADE697A00A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, 0x53544154450000019826E171EEF819DFF600005812, 10, NULL, NULL, '2025-07-20 06:58:07.173161', '2025-07-20 08:09:25.742597');
INSERT INTO `snapshots` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'LpaTmZqNPqWRY7M2R63MM', 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020207331313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020207331313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, 0x5354415445000001982685505351E5E42800001A0F, 2, NULL, NULL, '2025-07-20 06:16:22.697437', '2025-07-20 06:28:47.827694');
INSERT INTO `snapshots` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'wO-H59yCbXDnSGq5y3IKH', 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E5AFB9E68891E79A84E79A84E697A0E7BC9D33E694BEE79A84E58DB1E5BA9FE789A9E9A29DE79A84E58DB1E5BA9FE789A90A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A20202020202020200A202020202020202020200A2020202020200A20202020202020200A2020202020200A202020200A2020202020200A20202020202020200A0A20202020202020200A202020202020202020200A20202020202020202020202032303235E5B9B437E69C883232E697A5547565736461790A2020202020202020202020200A20202020202020202020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E5AFB9E68891E79A84E79A84E697A0E7BC9D33E694BEE79A84E58DB1E5BA9FE789A9E9A29DE79A84E58DB1E5BA9FE789A90A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020200A2020202020200A2020202020200A20202020202020200A20202020202020200A202020202020202020200A20200A202020200A20200A0A202020202020202020200A2020202020202020202020204672616D6520310A202020202020202020200A20202020202020200A20202020202020200A202020202020202020200A202020202020202020202020E6ADA46672616D65E5B7B2E68F92E585A5E4BD86E697A0E6B395E59CA8E697A0E8BEB9E7958CE6A8A1E5BC8FE4B88BE698BEE7A4BAE38082E58887E68DA2E588B0E9A1B5E99DA2E6A8A1E5BC8FE69FA5E79C8BE8AFA5E59D97E380820A202020202020202020200A20202020202020200A2020202020200A202020200A202020200A20202020202020200A2020202020200A202020200A2020202020202020202020200A202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A0A20202020202020200A20202020202020200A2020202020200A202020200A202020202020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A0A2020202020200A2020202020200A0A2020202020200A20202020555044010203040A2020202020200A20202020202020200A202020202020202020200A2020202020200A20202020202020200A2020202020200A202020200A2020202020200A20202020202020200A0A20202020202020200A202020202020202020200A20202020202020202020202032303235E5B9B437E69C883232E697A5547565736461790A2020202020202020202020200A20202020202020202020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E5AFB9E68891E79A84E79A84E697A0E7BC9D33E694BEE79A84E58DB1E5BA9FE789A9E9A29DE79A84E58DB1E5BA9FE789A90A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020200A2020202020200A2020202020200A20202020202020200A20202020202020200A202020202020202020200A20200A202020200A20200A0A202020202020202020200A2020202020202020202020204672616D6520310A202020202020202020200A20202020202020200A20202020202020200A202020202020202020200A202020202020202020202020E6ADA46672616D65E5B7B2E68F92E585A5E4BD86E697A0E6B395E59CA8E697A0E8BEB9E7958CE6A8A1E5BC8FE4B88BE698BEE7A4BAE38082E58887E68DA2E588B0E9A1B5E99DA2E6A8A1E5BC8FE69FA5E79C8BE8AFA5E59D97E380820A202020202020202020200A20202020202020200A2020202020200A202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A202020200A2020202020202020202020200A202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A0A20202020202020200A20202020202020200A2020202020200A202020200A202020202020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A0A2020202020200A2020202020200A0A2020202020200A20202020, 0x53544154450000019826EE26FDF250647100001B9F, 4, NULL, NULL, '2025-07-20 08:16:18.002124', '2025-07-20 08:23:18.525196');

-- ----------------------------
-- Table structure for subscriptions
-- ----------------------------
DROP TABLE IF EXISTS `subscriptions`;
CREATE TABLE `subscriptions`  (
  `amount` decimal(10, 2) NULL DEFAULT NULL,
  `cancel_at_period_end` bit(1) NULL DEFAULT NULL,
  `currency` varchar(3) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `quantity` int(11) NOT NULL,
  `canceled_at` datetime(6) NULL DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `current_period_end` datetime(6) NULL DEFAULT NULL,
  `current_period_start` datetime(6) NULL DEFAULT NULL,
  `end_time` datetime(6) NULL DEFAULT NULL,
  `next_bill_at` datetime(6) NULL DEFAULT NULL,
  `start_time` datetime(6) NOT NULL,
  `trial_end` datetime(6) NULL DEFAULT NULL,
  `trial_start` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `variant` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `cancel_reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `stripe_schedule_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `stripe_subscription_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `target_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `workspace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `billing_interval` enum('DAY','WEEK','MONTH','YEAR') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `metadata` json NULL,
  `plan` enum('FREE','PRO','TEAM','ENTERPRISE') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `recurring` enum('DAY','WEEK','MONTH','YEAR') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `status` enum('INCOMPLETE','INCOMPLETE_EXPIRED','TRIALING','ACTIVE','PAST_DUE','CANCELED','UNPAID','PAUSED') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_subscription_target_plan`(`target_id`, `plan`) USING BTREE,
  UNIQUE INDEX `UK_hrjab6j3njsjx6ua50ob6byeu`(`stripe_subscription_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of subscriptions
-- ----------------------------

-- ----------------------------
-- Table structure for updates
-- ----------------------------
DROP TABLE IF EXISTS `updates`;
CREATE TABLE `updates`  (
  `workspace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '工作空间ID',
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT 'GUID',
  `seq` int(11) NOT NULL COMMENT '序列号',
  `blob` longblob NOT NULL COMMENT '二进制数据',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  `created_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '创建者',
  PRIMARY KEY (`workspace_id`, `id`, `seq`) USING BTREE,
  INDEX `idx_update_workspace_created`(`workspace_id`, `created_at`) USING BTREE,
  INDEX `idx_update_created_by`(`created_by`) USING BTREE,
  CONSTRAINT `fk_update_created_by` FOREIGN KEY (`created_by`) REFERENCES `users` (`id`) ON DELETE SET NULL ON UPDATE RESTRICT,
  CONSTRAINT `fk_update_workspace` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '更新表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of updates
-- ----------------------------
INSERT INTO `updates` VALUES ('10ed579e-43a6-4754-a708-5beab282ee95', 'test-doc-1752718947713', 1, 0x0102030405060708090A, '2025-07-17 02:22:27.743780', NULL);
INSERT INTO `updates` VALUES ('10ed579e-43a6-4754-a708-5beab282ee95', 'test-doc-edit-1752720337161', 1, 0x010203313131313131333333, '2025-07-17 02:45:37.163944', NULL);
INSERT INTO `updates` VALUES ('10ed579e-43a6-4754-a708-5beab282ee95', 'test-doc-edit-1752720349576', 1, 0x010203343434343434343434, '2025-07-17 02:45:49.578340', NULL);
INSERT INTO `updates` VALUES ('10ed579e-43a6-4754-a708-5beab282ee95', 'test-doc-edit-1752723292953', 1, 0x0102033131313131313131313131, '2025-07-17 03:34:52.964654', NULL);
INSERT INTO `updates` VALUES ('8db6eaab-edb5-4b57-9b76-6aa1bca3161c', 'test-doc-edit-1752726832050', 1, 0x010203C2B73131313131, '2025-07-17 04:33:52.052502', NULL);
INSERT INTO `updates` VALUES ('8db6eaab-edb5-4b57-9b76-6aa1bca3161c', 'test-doc-edit-1752726853378', 1, 0x0102033333333333333333333333, '2025-07-17 04:34:13.379475', NULL);
INSERT INTO `updates` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', '5hEJ4IsCauFwimPr2RcaE', 1, 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203333333333333333333333333333330A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, '2025-07-20 14:55:49.090260', NULL);
INSERT INTO `updates` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'IfIGKlDIgPE3MWqt1tRvG', 1, 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, '2025-07-20 06:58:07.080731', NULL);
INSERT INTO `updates` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'IfIGKlDIgPE3MWqt1tRvG', 2, 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, '2025-07-20 06:59:39.329002', NULL);
INSERT INTO `updates` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'IfIGKlDIgPE3MWqt1tRvG', 3, 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, '2025-07-20 07:00:22.700385', NULL);
INSERT INTO `updates` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'IfIGKlDIgPE3MWqt1tRvG', 4, 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, '2025-07-20 07:05:24.024479', NULL);
INSERT INTO `updates` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'IfIGKlDIgPE3MWqt1tRvG', 5, 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, '2025-07-20 07:53:48.766135', NULL);
INSERT INTO `updates` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'IfIGKlDIgPE3MWqt1tRvG', 6, 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E68891E697A0E6958CE6B58BE8AF950A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, '2025-07-20 07:55:07.626574', NULL);
INSERT INTO `updates` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'IfIGKlDIgPE3MWqt1tRvG', 7, 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E68891E697A0E6958CE6B58BE8AF950A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, '2025-07-20 07:55:23.272980', NULL);
INSERT INTO `updates` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'IfIGKlDIgPE3MWqt1tRvG', 8, 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E68891E697A0E6958CE6B58BE8AF95E5919CE5919CE5919CE5919CE5919CE5919C0A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, '2025-07-20 07:59:02.011339', NULL);
INSERT INTO `updates` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'IfIGKlDIgPE3MWqt1tRvG', 9, 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E68891E697A0E6958CE6B58BE8AF95E5919CE5919CE5919CE5919CE5919CE5919CE5A4AAE783ADE789B9E789B90A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, '2025-07-20 08:06:13.512761', NULL);
INSERT INTO `updates` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'IfIGKlDIgPE3MWqt1tRvG', 10, 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E68891E697A0E6958CE6B58BE8AF95E5919CE5919CE5919CE5919CE5919CE5919CE5A4AAE783ADE789B9E789B9E783ADE783ADE697A00A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, '2025-07-20 08:09:25.622159', NULL);
INSERT INTO `updates` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'LpaTmZqNPqWRY7M2R63MM', 1, 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020207331313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, '2025-07-20 06:16:22.597265', NULL);
INSERT INTO `updates` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'LpaTmZqNPqWRY7M2R63MM', 2, 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020207331313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, '2025-07-20 06:28:47.789186', NULL);
INSERT INTO `updates` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'wO-H59yCbXDnSGq5y3IKH', 1, 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, '2025-07-20 08:16:17.957966', NULL);
INSERT INTO `updates` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'wO-H59yCbXDnSGq5y3IKH', 2, 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E5AFB9E68891E79A84E79A84E697A0E7BC9D33E694BEE79A84E58DB1E5BA9FE789A9E9A29DE79A84E58DB1E5BA9FE789A90A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, '2025-07-20 08:16:44.223307', NULL);
INSERT INTO `updates` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'wO-H59yCbXDnSGq5y3IKH', 3, 0x010203040A2020202020200A20202020202020200A202020202020202020200A2020202020200A20202020202020200A2020202020200A202020200A2020202020200A20202020202020200A0A20202020202020200A202020202020202020200A20202020202020202020202032303235E5B9B437E69C883232E697A5547565736461790A2020202020202020202020200A20202020202020202020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E5AFB9E68891E79A84E79A84E697A0E7BC9D33E694BEE79A84E58DB1E5BA9FE789A9E9A29DE79A84E58DB1E5BA9FE789A90A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020200A2020202020200A2020202020200A20202020202020200A20202020202020200A202020202020202020200A20200A202020200A20200A0A202020202020202020200A2020202020202020202020204672616D6520310A202020202020202020200A20202020202020200A20202020202020200A202020202020202020200A202020202020202020202020E6ADA46672616D65E5B7B2E68F92E585A5E4BD86E697A0E6B395E59CA8E697A0E8BEB9E7958CE6A8A1E5BC8FE4B88BE698BEE7A4BAE38082E58887E68DA2E588B0E9A1B5E99DA2E6A8A1E5BC8FE69FA5E79C8BE8AFA5E59D97E380820A202020202020202020200A20202020202020200A2020202020200A202020200A202020200A20202020202020200A2020202020200A202020200A2020202020202020202020200A202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A0A20202020202020200A20202020202020200A2020202020200A202020200A202020202020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A0A2020202020200A2020202020200A0A2020202020200A20202020, '2025-07-20 08:20:29.486734', NULL);
INSERT INTO `updates` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 'wO-H59yCbXDnSGq5y3IKH', 4, 0x010203040A2020202020200A20202020202020200A202020202020202020200A2020202020200A20202020202020200A2020202020200A202020200A2020202020200A20202020202020200A0A20202020202020200A202020202020202020200A20202020202020202020202032303235E5B9B437E69C883232E697A5547565736461790A2020202020202020202020200A20202020202020202020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E5AFB9E68891E79A84E79A84E697A0E7BC9D33E694BEE79A84E58DB1E5BA9FE789A9E9A29DE79A84E58DB1E5BA9FE789A90A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020200A2020202020200A2020202020200A20202020202020200A20202020202020200A202020202020202020200A20200A202020200A20200A0A202020202020202020200A2020202020202020202020204672616D6520310A202020202020202020200A20202020202020200A20202020202020200A202020202020202020200A202020202020202020202020E6ADA46672616D65E5B7B2E68F92E585A5E4BD86E697A0E6B395E59CA8E697A0E8BEB9E7958CE6A8A1E5BC8FE4B88BE698BEE7A4BAE38082E58887E68DA2E588B0E9A1B5E99DA2E6A8A1E5BC8FE69FA5E79C8BE8AFA5E59D97E380820A202020202020202020200A20202020202020200A2020202020200A202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A202020200A2020202020202020202020200A202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A0A20202020202020200A20202020202020200A2020202020200A202020200A202020202020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A0A2020202020200A2020202020200A0A2020202020200A20202020, '2025-07-20 08:23:18.509684', NULL);

-- ----------------------------
-- Table structure for user_connected_accounts
-- ----------------------------
DROP TABLE IF EXISTS `user_connected_accounts`;
CREATE TABLE `user_connected_accounts`  (
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NULL DEFAULT NULL,
  `updated_at` datetime(6) NOT NULL,
  `access_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `provider` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `provider_account_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `refresh_token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `scope` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_connected_account_user_id`(`user_id`) USING BTREE,
  INDEX `idx_connected_account_provider_id`(`provider_account_id`) USING BTREE,
  CONSTRAINT `FKpq2xfiq8f0x9fxmla6pt24f64` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_connected_accounts
-- ----------------------------

-- ----------------------------
-- Table structure for user_features
-- ----------------------------
DROP TABLE IF EXISTS `user_features`;
CREATE TABLE `user_features`  (
  `activated` bit(1) NOT NULL,
  `feature_id` int(11) NOT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` int(11) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expired_at` datetime(6) NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_feature_user_id`(`user_id`) USING BTREE,
  INDEX `idx_user_feature_name`(`name`) USING BTREE,
  INDEX `idx_user_feature_feature_id`(`feature_id`) USING BTREE,
  CONSTRAINT `FKn1bl34rxkodll6hm0dmrdijx0` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKrbw20p8pmx70wfn155ygsy3ll` FOREIGN KEY (`feature_id`) REFERENCES `features` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_features
-- ----------------------------

-- ----------------------------
-- Table structure for user_follows
-- ----------------------------
DROP TABLE IF EXISTS `user_follows`;
CREATE TABLE `user_follows`  (
  `id` bigint(20) NOT NULL AUTO_INCREMENT COMMENT '关注ID',
  `follower_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '关注者ID',
  `followee_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '被关注者ID',
  `created_at` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_follower_followee`(`follower_id`, `followee_id`) USING BTREE,
  INDEX `idx_follower_id`(`follower_id`) USING BTREE,
  INDEX `idx_followee_id`(`followee_id`) USING BTREE,
  INDEX `idx_created_at`(`created_at`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户关注表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_follows
-- ----------------------------

-- ----------------------------
-- Table structure for user_quotas
-- ----------------------------
DROP TABLE IF EXISTS `user_quotas`;
CREATE TABLE `user_quotas`  (
  `copilot_action_limit` int(11) NOT NULL,
  `history_period` int(11) NOT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `is_lifetime` bit(1) NOT NULL,
  `is_pro` bit(1) NOT NULL,
  `member_limit` int(11) NOT NULL,
  `blob_limit` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expired_at` datetime(6) NULL DEFAULT NULL,
  `storage_quota` bigint(20) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `plan_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_epfvahpv00kam8y9u72mhyigs`(`user_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_quotas
-- ----------------------------

-- ----------------------------
-- Table structure for user_sessions
-- ----------------------------
DROP TABLE IF EXISTS `user_sessions`;
CREATE TABLE `user_sessions`  (
  `created_at` datetime(6) NOT NULL,
  `expires_at` datetime(6) NULL DEFAULT NULL,
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `session_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_session_user`(`session_id`, `user_id`) USING BTREE,
  INDEX `FK8klxsgb8dcjjklmqebqp1twd5`(`user_id`) USING BTREE,
  CONSTRAINT `FK8klxsgb8dcjjklmqebqp1twd5` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKmj30g53179g37e767fso599i5` FOREIGN KEY (`session_id`) REFERENCES `multiple_users_sessions` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_sessions
-- ----------------------------
INSERT INTO `user_sessions` VALUES ('2025-07-16 01:59:22.479836', '2025-07-17 01:59:22.473837', '0036049f-57be-4514-970d-3c51d749be99', 'ae5d4023-dc78-4150-afcc-b078c88a0660', 'f13bae0a-e0c9-4941-bdbc-b353cc9fae84');
INSERT INTO `user_sessions` VALUES ('2025-07-15 14:04:38.792005', '2025-07-16 14:04:38.790009', '0ae0aac4-0872-40e7-8eb7-3248a61259f9', '0ebec862-9752-4817-bf47-bd823f273000', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-15 14:03:59.051980', '2025-07-16 14:03:59.044925', '0dc73b4c-f350-4160-9b61-368cb81ac23f', '76018301-7ee9-4428-af84-143306b76956', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-15 14:04:38.603958', '2025-07-16 14:04:38.601963', '12efa1c7-a748-4121-ba59-9653519b68c1', '0f3dd133-3167-46f6-88ca-58e3a4b96ec7', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-16 02:04:35.211016', '2025-07-17 02:04:35.208815', '25192b59-d973-4628-b14b-3ff40c37b75f', '07a93cc5-d627-4b45-a6bd-a507e2b68d5a', 'b5bac2d8-a600-4ea6-afa0-e0188c95f4a4');
INSERT INTO `user_sessions` VALUES ('2025-07-17 00:01:29.978314', '2025-07-18 00:01:29.976306', '3933890e-ffd8-47cb-8432-d9d1504ca6f6', 'c2ca66d1-8cb8-4d8b-9a6d-0309dbac1672', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-16 01:13:35.441171', '2025-07-17 01:13:35.430612', '3a9c0790-36d3-4808-8f14-81dbf4924c3e', '917f5770-fe61-41d9-8b9d-2b759aa7b243', 'b5bac2d8-a600-4ea6-afa0-e0188c95f4a4');
INSERT INTO `user_sessions` VALUES ('2025-07-17 00:01:29.785347', '2025-07-18 00:01:29.780907', '3c138ed6-aca0-4074-86eb-21f6af5ae84d', 'a995dc42-d1c5-4769-b1b7-1d4ebc8ffa92', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-16 02:32:58.318956', '2025-07-17 02:32:58.315951', '49215cdb-228b-4f6b-88ca-ef090ffc1b4e', 'c688394b-88dd-4a27-8880-753d14065089', 'b5bac2d8-a600-4ea6-afa0-e0188c95f4a4');
INSERT INTO `user_sessions` VALUES ('2025-07-17 00:01:32.052674', '2025-07-18 00:01:32.047785', '4ded77d3-35b3-4887-ba57-0f9290d29236', '2d1b97c3-efe6-40d6-a14d-28d812bb19bb', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-16 00:43:17.249424', '2025-07-17 00:43:17.246428', '50559d6e-2221-404e-95ea-9e538f549bf9', '1d2d76a7-75f1-43f4-a8a9-dbff4e263c80', 'b5bac2d8-a600-4ea6-afa0-e0188c95f4a4');
INSERT INTO `user_sessions` VALUES ('2025-07-16 01:51:05.771411', '2025-07-17 01:51:05.767390', '51b78dce-d593-4d40-8fd4-dad512d85636', '2f56bc9a-07f1-4db8-8b4f-df8f6b32cf55', 'b5bac2d8-a600-4ea6-afa0-e0188c95f4a4');
INSERT INTO `user_sessions` VALUES ('2025-07-17 00:01:31.494352', '2025-07-18 00:01:31.490312', '568f629e-a9a0-4c87-a074-eb66283c38ff', '58fc08aa-dc85-4b28-8a87-ee08d2720dae', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-16 01:44:54.511470', '2025-07-17 01:44:54.501665', '576fd7c1-2b70-4bdc-96ee-ae1ab2b564c4', 'ae07a39a-8327-4d0b-865d-da7e96cb1a17', 'bbede7d2-50d0-4692-ad8d-24d263139b30');
INSERT INTO `user_sessions` VALUES ('2025-07-16 02:15:25.025906', '2025-07-17 02:15:25.023901', '6292a1fd-0d18-42c0-9a3b-714615dcf6ff', 'ce0d5abb-e029-49fb-ae9b-8f62c509d9f0', 'b5bac2d8-a600-4ea6-afa0-e0188c95f4a4');
INSERT INTO `user_sessions` VALUES ('2025-07-17 00:01:30.352093', '2025-07-18 00:01:30.347570', '653f7378-9b03-49e3-9ff9-59b577a34d29', '4cdbf785-47fe-4eb4-870b-d1597590fcd1', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-17 00:01:31.125771', '2025-07-18 00:01:31.121109', '66b56a71-7966-436a-939c-7e78f75e635b', '5d0077e6-a0c0-4456-bc2a-aad0e8da3c88', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-15 14:04:38.457463', '2025-07-16 14:04:38.455484', '71fed6b9-e350-4bb6-983b-44895b92fbd8', '1a6056f4-2266-47fc-be43-ef37561b27cf', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-15 14:06:19.435132', '2025-07-16 14:06:19.432519', '7520d6bc-7b61-4841-b4d1-0a60987a61f8', 'f391ef6a-accf-4b0f-b707-5614f3b16609', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-16 01:41:07.319909', '2025-07-17 01:41:07.317878', '78d660ec-5eaf-4854-ba3c-b4ef178403b7', 'f3ec5431-39b4-430e-8fbd-8cb0f8c2c0d4', 'b5bac2d8-a600-4ea6-afa0-e0188c95f4a4');
INSERT INTO `user_sessions` VALUES ('2025-07-15 14:04:38.008563', '2025-07-16 14:04:38.006566', '81a916e8-51f2-466d-8c1e-a34846b71dfb', '2731dd62-4dc9-4b3e-8930-e3b5dccb01f6', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-17 00:00:54.495207', '2025-07-18 00:00:54.470157', '85dd64e0-addf-4834-9d36-825507f80ce2', '1e28558d-cd39-4327-871c-aad02e39ba4a', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-15 14:04:24.107671', '2025-07-16 14:04:24.104052', '8fdad6a4-d23c-41ca-9841-0c0c98a474ee', '89684009-7774-4fb1-a5df-4b69ed61cea4', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-16 02:48:08.971439', '2025-07-17 02:48:08.966485', '909d2594-bac1-4745-9266-ad4cf67dda7e', '7ef08794-ddec-4343-979b-c5a705acd956', 'b5bac2d8-a600-4ea6-afa0-e0188c95f4a4');
INSERT INTO `user_sessions` VALUES ('2025-07-17 00:01:30.520414', '2025-07-18 00:01:30.518392', '9642baac-61e1-4e55-8020-2a1b1b48dd97', '2cae7257-5bbe-48d9-8afe-49ea88388be5', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-17 00:01:28.666294', '2025-07-18 00:01:28.663265', '9bb55fbb-ce72-4682-a9d7-4e7608a5c2a5', 'e82d136a-a037-48e6-9b08-6f027e5d94ef', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-17 00:01:30.733589', '2025-07-18 00:01:30.729215', '9de8aa29-75ee-4802-86f3-c4e5e5e26c3e', '0b6671b3-d4b0-4634-9286-6b795dad404e', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-17 00:01:31.310661', '2025-07-18 00:01:31.302709', 'a32edad5-a427-474c-87f3-b5d5d4238a97', '3c3bace7-ae31-4492-864d-46b7bbaefd26', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-16 02:10:50.505849', '2025-07-17 02:10:50.501128', 'a38738fe-7826-4ed0-b4f1-8bcc6a9bec40', '884559a3-66c8-450e-863d-cc0729f1cc59', 'b5bac2d8-a600-4ea6-afa0-e0188c95f4a4');
INSERT INTO `user_sessions` VALUES ('2025-07-15 14:04:38.224085', '2025-07-16 14:04:38.221044', 'a7ead630-f3c7-4629-a0e3-b03c69ade7c7', 'a4006221-24a9-408a-a7a0-cfe0a9be0ccc', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-17 00:01:30.160475', '2025-07-18 00:01:30.156517', 'ab20879a-a0a1-4f05-9641-00380977e015', '5dc5ed79-46e9-4f0a-9ace-a2c28aab84b3', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-17 02:53:11.516626', '2025-07-18 02:53:11.502146', 'ab8b72a7-e816-4dd7-ad2f-c7010db94125', '595b0335-2f17-4d11-adac-f3736df6e83a', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-17 00:01:29.597368', '2025-07-18 00:01:29.593758', 'ad7395e7-e1d9-4550-b4b4-4b2e3762ee38', '3fe779ce-6261-43eb-b915-2198a86c1eee', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-17 00:01:31.711752', '2025-07-18 00:01:31.709756', 'b12ce18b-cd84-4e39-8043-b7094b4c7e00', 'ba76b93d-b99e-447c-9dbe-6f5d10f7c481', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-16 02:30:34.252856', '2025-07-17 02:30:34.246308', 'b5f8fd4c-211d-4347-9881-2a3c101f8da5', 'e4011025-2182-46f4-81cc-a0fc15c91c17', 'b5bac2d8-a600-4ea6-afa0-e0188c95f4a4');
INSERT INTO `user_sessions` VALUES ('2025-07-16 01:40:09.971586', '2025-07-17 01:40:09.966046', 'cdbe1d4c-fa5d-4c22-82b1-05d31755aa58', '38900528-671a-4cd2-b152-f03bedf76c4f', 'b5bac2d8-a600-4ea6-afa0-e0188c95f4a4');
INSERT INTO `user_sessions` VALUES ('2025-07-15 14:04:00.327794', '2025-07-16 14:04:00.324931', 'd1074794-d413-41da-b7f5-1b93f4283dd6', 'decfacfe-bae8-4201-a2e6-f45f3e174b59', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-17 00:01:08.555951', '2025-07-18 00:01:08.553478', 'd1326df9-ccf1-446a-ae70-6e22ae2331af', '492f4d10-1c4a-495d-bea4-7aed85a0e665', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-17 00:01:30.929367', '2025-07-18 00:01:30.926361', 'd3c9a1da-5ef3-4ab9-9303-fedbebe723bf', '1805d6ee-e8a3-4946-b4c0-4b11be58e871', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-16 02:01:31.295732', '2025-07-17 02:01:31.290686', 'de37ae09-a2e8-49b4-82f6-3f7b32b60e8c', '7c0531f9-c361-41df-8f39-5935e638288a', 'b5bac2d8-a600-4ea6-afa0-e0188c95f4a4');
INSERT INTO `user_sessions` VALUES ('2025-07-15 14:03:47.916880', '2025-07-16 14:03:47.912874', 'e5759a2f-4eaf-4f3f-8f50-d5fe6eba95e2', 'afb78ed1-de60-482c-979f-5cfc09bce4c8', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');
INSERT INTO `user_sessions` VALUES ('2025-07-16 02:21:06.862360', '2025-07-17 02:21:06.860353', 'ec6b6530-fd44-429a-bc49-2732b39d772e', '52f08692-fb1c-4db2-a841-7e8c08c9bb1c', 'b5bac2d8-a600-4ea6-afa0-e0188c95f4a4');
INSERT INTO `user_sessions` VALUES ('2025-07-15 04:24:34.873819', '2025-07-16 04:24:34.870823', 'ee5fca45-c825-4f8e-9e09-21af3d110726', '05cd336a-9e29-4ca5-8249-b83ca9b3f57c', '4bba3926-3e00-4aa5-87b6-c20eaa9448a1');

-- ----------------------------
-- Table structure for user_settings
-- ----------------------------
DROP TABLE IF EXISTS `user_settings`;
CREATE TABLE `user_settings`  (
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID，主键',
  `settings` json NULL COMMENT '设置，JSON类型',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  `updated_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6) COMMENT '更新时间',
  PRIMARY KEY (`user_id`) USING BTREE,
  CONSTRAINT `fk_user_settings_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户设置表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_settings
-- ----------------------------

-- ----------------------------
-- Table structure for user_snapshots
-- ----------------------------
DROP TABLE IF EXISTS `user_snapshots`;
CREATE TABLE `user_snapshots`  (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '主键，UUID',
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户ID',
  `type` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '类型',
  `data` json NULL COMMENT '数据，JSON类型',
  `created_at` datetime(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) COMMENT '创建时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_snapshot_user_created`(`user_id`, `created_at`) USING BTREE,
  INDEX `idx_user_snapshot_type`(`type`) USING BTREE,
  CONSTRAINT `fk_user_snapshot_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户快照表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_snapshots
-- ----------------------------

-- ----------------------------
-- Table structure for user_stripe_customers
-- ----------------------------
DROP TABLE IF EXISTS `user_stripe_customers`;
CREATE TABLE `user_stripe_customers`  (
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `customer_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`user_id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of user_stripe_customers
-- ----------------------------

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `email` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `email_verified` datetime(3) NULL DEFAULT NULL,
  `avatar_url` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `registered` tinyint(1) NOT NULL DEFAULT 1,
  `disabled` tinyint(1) NOT NULL DEFAULT 0,
  `enabled` tinyint(1) NOT NULL DEFAULT 1,
  `stripe_customer_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `registered_at` datetime(3) NULL DEFAULT NULL,
  `updated_at` datetime(3) NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `email`(`email`) USING BTREE,
  UNIQUE INDEX `UK_6dotkott2kjsp8vw4d0m25fb7`(`email`) USING BTREE,
  INDEX `idx_user_email`(`email`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES ('0860eb91-81c8-4444-9540-5e79ce2d58e8', '管理员', 'admin@example.com', '2025-07-13 07:48:37.027', NULL, '2025-07-13 07:48:37.029', '$2a$12$wuJ.DMpqKPPeVY6BSVxis.jVirsOKEjAchika//ps9MS6eFc7PCHO', 1, 0, 1, NULL, NULL, NULL);
INSERT INTO `users` VALUES ('4bba3926-3e00-4aa5-87b6-c20eaa9448a1', '测试用户', 'test@example.com', '2025-07-13 07:48:36.656', NULL, '2025-07-13 07:48:36.709', '$2a$12$4Up9XoMkYwo8cywSa7pOgOhX.2AxIxdTDtLD6pIjCagPezazYLUEW', 1, 0, 1, NULL, NULL, NULL);
INSERT INTO `users` VALUES ('88dd05bc-7c72-4fc9-a897-72b144bc63b3', '普通用户', 'user@example.com', '2025-07-13 07:48:37.309', NULL, '2025-07-13 07:48:37.311', '$2a$12$CnI0GtLJ9pS6eM6FW0xojOntGR/dvxN/N757aBDW4tLnAyaq/327m', 1, 0, 1, NULL, NULL, NULL);
INSERT INTO `users` VALUES ('b5bac2d8-a600-4ea6-afa0-e0188c95f4a4', '管理员', 'admin@12380099024477266.com', '2025-07-16 00:40:20.460', NULL, '2025-07-16 00:40:20.508', '$2a$12$kKI0SNXKgLgJDnFrYKgLi.uDrn/Hn7cbBOpx148NoQKctBkY0LyPK', 1, 0, 1, NULL, NULL, NULL);
INSERT INTO `users` VALUES ('bbede7d2-50d0-4692-ad8d-24d263139b30', '新管理员', 'newadmin', '2025-07-16 01:44:01.576', NULL, '2025-07-16 01:44:01.586', '$2a$12$4GyfY4c.CLkgI7xQB1VYSeJ2GczfgkXnBz1Q4dyjFRQIhx9hcyPbO', 1, 0, 1, NULL, NULL, NULL);
INSERT INTO `users` VALUES ('f13bae0a-e0c9-4941-bdbc-b353cc9fae84', 'Admin User', 'admin', '2025-07-16 01:59:15.169', NULL, '2025-07-16 01:59:15.180', '$2a$12$tkBRP7Ua9oiYfzS31OVBUOCQgjK6UuWSd7yv/HTC4dzfzNA6DNQ4e', 1, 0, 1, NULL, NULL, NULL);

-- ----------------------------
-- Table structure for verification_tokens
-- ----------------------------
DROP TABLE IF EXISTS `verification_tokens`;
CREATE TABLE `verification_tokens`  (
  `type` smallint(6) NOT NULL,
  `expires_at` datetime(6) NOT NULL,
  `credential` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `token` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`token`) USING BTREE,
  UNIQUE INDEX `uk_token_type`(`type`, `token`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of verification_tokens
-- ----------------------------

-- ----------------------------
-- Table structure for workspace_doc_user_roles
-- ----------------------------
DROP TABLE IF EXISTS `workspace_doc_user_roles`;
CREATE TABLE `workspace_doc_user_roles`  (
  `workspace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `doc_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `type` smallint(6) NOT NULL,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  PRIMARY KEY (`workspace_id`, `doc_id`, `user_id`) USING BTREE,
  INDEX `user_id`(`user_id`) USING BTREE,
  CONSTRAINT `workspace_doc_user_roles_ibfk_1` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT,
  CONSTRAINT `workspace_doc_user_roles_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of workspace_doc_user_roles
-- ----------------------------

-- ----------------------------
-- Table structure for workspace_features
-- ----------------------------
DROP TABLE IF EXISTS `workspace_features`;
CREATE TABLE `workspace_features`  (
  `activated` bit(1) NOT NULL,
  `feature_id` int(11) NOT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `type` int(11) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expired_at` datetime(6) NULL DEFAULT NULL,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `reason` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `workspace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `configs` json NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_workspace_feature_workspace_id`(`workspace_id`) USING BTREE,
  INDEX `idx_workspace_feature_name`(`name`) USING BTREE,
  INDEX `idx_workspace_feature_feature_id`(`feature_id`) USING BTREE,
  CONSTRAINT `FKfx0l22j3v2wmt1xb86btm5jwu` FOREIGN KEY (`feature_id`) REFERENCES `features` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKo5q29p3pv3vlcdbbe7oj4e9vd` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of workspace_features
-- ----------------------------

-- ----------------------------
-- Table structure for workspace_members
-- ----------------------------
DROP TABLE IF EXISTS `workspace_members`;
CREATE TABLE `workspace_members`  (
  `acceptedAt` datetime(6) NULL DEFAULT NULL,
  `createdAt` datetime(6) NULL DEFAULT NULL,
  `updatedAt` datetime(6) NULL DEFAULT NULL,
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `userId` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `workspaceId` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` enum('Accepted','Pending','Rejected') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of workspace_members
-- ----------------------------

-- ----------------------------
-- Table structure for workspace_page_permissions
-- ----------------------------
DROP TABLE IF EXISTS `workspace_page_permissions`;
CREATE TABLE `workspace_page_permissions`  (
  `createdAt` datetime(6) NULL DEFAULT NULL,
  `updatedAt` datetime(6) NULL DEFAULT NULL,
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `pageId` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `userId` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `workspaceId` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `permission` enum('OWNER','EDITOR','VIEWER','NONE') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of workspace_page_permissions
-- ----------------------------

-- ----------------------------
-- Table structure for workspace_page_user_permissions
-- ----------------------------
DROP TABLE IF EXISTS `workspace_page_user_permissions`;
CREATE TABLE `workspace_page_user_permissions`  (
  `type` smallint(6) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `page_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `workspace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`page_id`, `user_id`, `workspace_id`) USING BTREE,
  INDEX `FK3eeiga4ayb1kvscat9u43xdh5`(`user_id`) USING BTREE,
  INDEX `FKb32b5nbr1nt6cab94msgoqisi`(`workspace_id`) USING BTREE,
  CONSTRAINT `FK3eeiga4ayb1kvscat9u43xdh5` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKb32b5nbr1nt6cab94msgoqisi` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of workspace_page_user_permissions
-- ----------------------------

-- ----------------------------
-- Table structure for workspace_pages
-- ----------------------------
DROP TABLE IF EXISTS `workspace_pages`;
CREATE TABLE `workspace_pages`  (
  `blocked` bit(1) NOT NULL,
  `default_role` smallint(6) NOT NULL,
  `mode` smallint(6) NOT NULL,
  `public` bit(1) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `page_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `summary` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `workspace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `binary_data` longblob NULL COMMENT '二进制数据',
  `public_mode` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '公开模式',
  `public_permission` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '公开权限',
  `community_shared` tinyint(1) NULL DEFAULT 0 COMMENT '是否分享到社区',
  `community_permission` enum('PUBLIC','COLLABORATOR','ADMIN','CUSTOM') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '社区访问权限：PUBLIC-所有工作空间成员可见，COLLABORATOR-协作者及以上权限可见，ADMIN-仅管理员和所有者可见，CUSTOM-自定义用户可见',
  `community_shared_at` timestamp NULL DEFAULT NULL COMMENT '分享到社区的时间',
  `community_title` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '社区显示标题（可与原文档标题不同）',
  `community_description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL COMMENT '社区显示描述（文档简介）',
  `community_view_count` int(11) NULL DEFAULT 0 COMMENT '社区浏览次数统计',
  PRIMARY KEY (`page_id`, `workspace_id`) USING BTREE,
  INDEX `idx_community_shared`(`community_shared`, `community_permission`) USING BTREE,
  INDEX `idx_community_shared_at`(`community_shared_at`) USING BTREE,
  INDEX `idx_workspace_community`(`workspace_id`, `community_shared`) USING BTREE,
  INDEX `idx_community_view_count`(`community_shared`, `community_view_count`) USING BTREE,
  CONSTRAINT `FKltcdo7dj4ovdkxhwx36y6nkwx` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of workspace_pages
-- ----------------------------
INSERT INTO `workspace_pages` VALUES (b'0', 30, 0, b'0', '2025-07-20 14:55:49.110288', '2025-07-20 14:55:49.110288', '5hEJ4IsCauFwimPr2RcaE', NULL, 'Untitled Document', 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203333333333333333333333333333330A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, NULL, NULL, 0, NULL, NULL, NULL, NULL, 0);
INSERT INTO `workspace_pages` VALUES (b'0', 30, 0, b'0', '2025-07-20 06:58:07.157729', '2025-07-20 08:09:25.718044', 'IfIGKlDIgPE3MWqt1tRvG', NULL, 'Untitled Document', 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E68891E697A0E6958CE6B58BE8AF950A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E68891E697A0E6958CE6B58BE8AF950A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E68891E697A0E6958CE6B58BE8AF95E5919CE5919CE5919CE5919CE5919CE5919C0A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E68891E697A0E6958CE6B58BE8AF95E5919CE5919CE5919CE5919CE5919CE5919CE5A4AAE783ADE789B9E789B90A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E68891E697A0E6958CE6B58BE8AF95E5919CE5919CE5919CE5919CE5919CE5919CE5A4AAE783ADE789B9E789B9E783ADE783ADE697A00A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, NULL, NULL, 0, NULL, NULL, NULL, NULL, 0);
INSERT INTO `workspace_pages` VALUES (b'0', 30, 0, b'0', '2025-07-20 06:16:22.662178', '2025-07-20 06:28:47.807247', 'LpaTmZqNPqWRY7M2R63MM', NULL, 'Untitled Document', 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020207331313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020207331313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020203131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020, NULL, NULL, 0, NULL, NULL, NULL, NULL, 0);
INSERT INTO `workspace_pages` VALUES (b'0', 30, 0, b'0', '2025-07-17 02:22:27.906048', '2025-07-17 02:22:27.907046', 'test-doc-1752718947713', NULL, 'Untitled Document', '10ed579e-43a6-4754-a708-5beab282ee95', 0x0102030405060708090A, NULL, NULL, 0, NULL, NULL, NULL, NULL, 0);
INSERT INTO `workspace_pages` VALUES (b'0', 30, 0, b'0', '2025-07-17 02:45:37.185023', '2025-07-17 02:45:37.185023', 'test-doc-edit-1752720337161', NULL, 'Untitled Document', '10ed579e-43a6-4754-a708-5beab282ee95', 0x010203313131313131333333, NULL, NULL, 0, NULL, NULL, NULL, NULL, 0);
INSERT INTO `workspace_pages` VALUES (b'0', 30, 0, b'0', '2025-07-17 02:45:49.591122', '2025-07-17 02:45:49.591122', 'test-doc-edit-1752720349576', NULL, 'Untitled Document', '10ed579e-43a6-4754-a708-5beab282ee95', 0x010203343434343434343434, NULL, NULL, 0, NULL, NULL, NULL, NULL, 0);
INSERT INTO `workspace_pages` VALUES (b'0', 30, 0, b'0', '2025-07-17 03:34:53.090320', '2025-07-17 03:34:53.091347', 'test-doc-edit-1752723292953', NULL, 'Untitled Document', '10ed579e-43a6-4754-a708-5beab282ee95', 0x0102033131313131313131313131, NULL, NULL, 0, NULL, NULL, NULL, NULL, 0);
INSERT INTO `workspace_pages` VALUES (b'0', 30, 0, b'0', '2025-07-17 04:33:52.087105', '2025-07-17 04:33:52.087105', 'test-doc-edit-1752726832050', NULL, 'Untitled Document', '8db6eaab-edb5-4b57-9b76-6aa1bca3161c', 0x010203C2B73131313131, NULL, NULL, 0, NULL, NULL, NULL, NULL, 0);
INSERT INTO `workspace_pages` VALUES (b'0', 30, 0, b'0', '2025-07-17 04:34:13.391352', '2025-07-17 04:34:13.392417', 'test-doc-edit-1752726853378', NULL, 'Untitled Document', '8db6eaab-edb5-4b57-9b76-6aa1bca3161c', 0x0102033333333333333333333333, NULL, NULL, 0, NULL, NULL, NULL, NULL, 0);
INSERT INTO `workspace_pages` VALUES (b'0', 30, 0, b'0', '2025-07-20 08:16:17.971865', '2025-07-20 08:23:18.518685', 'wO-H59yCbXDnSGq5y3IKH', NULL, 'Untitled Document', 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 0x010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E5AFB9E68891E79A84E79A84E697A0E7BC9D33E694BEE79A84E58DB1E5BA9FE789A9E9A29DE79A84E58DB1E5BA9FE789A90A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A20202020200A20202020555044010203040A2020202020200A20202020202020200A202020202020202020200A2020202020200A20202020202020200A2020202020200A202020200A2020202020200A20202020202020200A0A20202020202020200A202020202020202020200A20202020202020202020202032303235E5B9B437E69C883232E697A5547565736461790A2020202020202020202020200A20202020202020202020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E5AFB9E68891E79A84E79A84E697A0E7BC9D33E694BEE79A84E58DB1E5BA9FE789A9E9A29DE79A84E58DB1E5BA9FE789A90A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020200A2020202020200A2020202020200A20202020202020200A20202020202020200A202020202020202020200A20200A202020200A20200A0A202020202020202020200A2020202020202020202020204672616D6520310A202020202020202020200A20202020202020200A20202020202020200A202020202020202020200A202020202020202020202020E6ADA46672616D65E5B7B2E68F92E585A5E4BD86E697A0E6B395E59CA8E697A0E8BEB9E7958CE6A8A1E5BC8FE4B88BE698BEE7A4BAE38082E58887E68DA2E588B0E9A1B5E99DA2E6A8A1E5BC8FE69FA5E79C8BE8AFA5E59D97E380820A202020202020202020200A20202020202020200A2020202020200A202020200A202020200A20202020202020200A2020202020200A202020200A2020202020202020202020200A202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A0A20202020202020200A20202020202020200A2020202020200A202020200A202020202020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A0A2020202020200A2020202020200A0A2020202020200A20202020555044010203040A2020202020200A20202020202020200A202020202020202020200A2020202020200A20202020202020200A2020202020200A202020200A2020202020200A20202020202020200A0A20202020202020200A202020202020202020200A20202020202020202020202032303235E5B9B437E69C883232E697A5547565736461790A2020202020202020202020200A20202020202020202020202020200A2020202020200A20202020202020200A202020202020202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020313131313131313131313131313131313131313131313131310A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A20202020202020202020E5AFB9E68891E79A84E79A84E697A0E7BC9D33E694BEE79A84E58DB1E5BA9FE789A9E9A29DE79A84E58DB1E5BA9FE789A90A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A20202020200A2020202020200A2020202020200A20202020202020200A20202020202020200A202020202020202020200A20200A202020200A20200A0A202020202020202020200A2020202020202020202020204672616D6520310A202020202020202020200A20202020202020200A20202020202020200A202020202020202020200A202020202020202020202020E6ADA46672616D65E5B7B2E68F92E585A5E4BD86E697A0E6B395E59CA8E697A0E8BEB9E7958CE6A8A1E5BC8FE4B88BE698BEE7A4BAE38082E58887E68DA2E588B0E9A1B5E99DA2E6A8A1E5BC8FE69FA5E79C8BE8AFA5E59D97E380820A202020202020202020200A20202020202020200A2020202020200A202020200A202020200A2020202020200A2020202020200A2020202020200A20202020202020202E616666696E652D7061726167726170682D626C6F636B2D636F6E7461696E65725B646174612D6861732D636F6C6C61707365642D7369626C696E67733D2766616C7365275D0A20202020202020202020616666696E652D7061726167726170682D68656164696E672D69636F6E0A202020202020202020202E68656164696E672D69636F6E207B0A202020202020202020207472616E73666F726D3A207472616E736C61746558282D34387078293B0A20202020202020207D0A2020202020200A2020202020200A20202020202020200A202020202020202020200A202020202020202020200A202020202020202020200A2020202020202020E2808B0A2020202020200A202020202020202020200A202020202020202020202020202020200A202020202020202020202020202020202020E8BE93E585A520272F2720E8B083E794A8E591BDE4BBA40A202020202020202020202020202020200A20202020202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A2020202020200A202020200A2020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A202020200A2020202020202020202020200A202020202020202020200A20202020202020200A0A20202020202020200A2020202020200A202020200A0A20202020202020200A20202020202020200A2020202020200A202020200A202020202020202020200A2020202020200A2020202020200A202020200A20202020202020200A2020202020200A0A2020202020200A2020202020200A0A2020202020200A20202020, NULL, NULL, 0, NULL, NULL, NULL, NULL, 0);

-- ----------------------------
-- Table structure for workspace_quotas
-- ----------------------------
DROP TABLE IF EXISTS `workspace_quotas`;
CREATE TABLE `workspace_quotas`  (
  `copilot_action_limit` int(11) NOT NULL,
  `history_period` int(11) NOT NULL,
  `id` int(11) NOT NULL AUTO_INCREMENT,
  `is_pro` bit(1) NOT NULL,
  `is_team` bit(1) NOT NULL,
  `member_limit` int(11) NOT NULL,
  `seat_count` int(11) NOT NULL,
  `blob_limit` bigint(20) NOT NULL,
  `created_at` datetime(6) NOT NULL,
  `expired_at` datetime(6) NULL DEFAULT NULL,
  `per_seat_quota` bigint(20) NOT NULL,
  `storage_quota` bigint(20) NOT NULL,
  `updated_at` datetime(6) NOT NULL,
  `plan_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `workspace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `UK_6nxsjvqcmkr9x9q33x1r4ms16`(`workspace_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of workspace_quotas
-- ----------------------------

-- ----------------------------
-- Table structure for workspace_user_roles
-- ----------------------------
DROP TABLE IF EXISTS `workspace_user_roles`;
CREATE TABLE `workspace_user_roles`  (
  `acceptedAt` datetime(6) NULL DEFAULT NULL,
  `createdAt` datetime(6) NULL DEFAULT NULL,
  `updatedAt` datetime(6) NULL DEFAULT NULL,
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `inviterId` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `inviter_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `userId` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `user_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `workspaceId` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `workspace_id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `source` enum('EMAIL','LINK','SELF_JOIN') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `status` enum('ACCEPTED','PENDING','REJECTED') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `type` enum('OWNER','ADMIN','COLLABORATOR','EXTERNAL') CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `FKq8lsxu1j1qy60g082n9j2jv10`(`inviter_id`) USING BTREE,
  INDEX `FKkkyncw9vg4ghjn4snd50l10i0`(`user_id`) USING BTREE,
  INDEX `FKjjsr14qypp9vyiyqtxaqwyrao`(`workspace_id`) USING BTREE,
  CONSTRAINT `FKjjsr14qypp9vyiyqtxaqwyrao` FOREIGN KEY (`workspace_id`) REFERENCES `workspaces` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKkkyncw9vg4ghjn4snd50l10i0` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `FKq8lsxu1j1qy60g082n9j2jv10` FOREIGN KEY (`inviter_id`) REFERENCES `users` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of workspace_user_roles
-- ----------------------------
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-14 07:21:42.131993', '2025-07-14 07:21:42.131993', '0c2b6a44-0113-436b-87ca-e53c569e41d9', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, '10ed579e-43a6-4754-a708-5beab282ee95', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-20 03:14:55.609044', '2025-07-20 03:14:55.609044', '12aa0ed4-a537-4088-b0dd-fd90f7fbeb4d', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, '2d0366aa-f4ff-407d-8aaf-a0703b7d3b98', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-14 06:08:26.811856', '2025-07-14 06:08:26.811856', '15ebf465-97b4-432c-bfe3-fd13c25bbdca', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, 'b13f06ac-c685-4786-8fad-e121e8db3978', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-16 05:41:22.430260', '2025-07-16 05:41:22.430260', '25e24894-5dbd-4d42-a5f3-c39e89a6ffc5', NULL, NULL, 'b5bac2d8-a600-4ea6-afa0-e0188c95f4a4', NULL, 'e41f86bb-e35f-408d-8c15-9775bde5c178', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-19 05:44:10.568988', '2025-07-19 05:44:10.568988', '296e0f7a-378b-4d7f-a928-a831cc8b6e7e', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, '991b6e05-87f8-4f70-848e-6e4d1215eef0', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-14 06:03:52.063326', '2025-07-14 06:03:52.063326', '34573168-8d6c-40ef-92dd-59fac66e03ae', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, '8b84a46d-ffd8-4803-82ab-c2d3110ffdd6', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-14 06:46:27.489801', '2025-07-14 06:46:27.489801', '59e88109-4e0b-4789-b8ad-1affa9890eca', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, '5c647e19-61b3-4bdc-9e5b-a59878f1a6fd', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-20 03:43:29.900408', '2025-07-20 03:43:29.900408', '649b71ca-b074-49dc-b906-c263207f9814', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, 'd33eccd3-3d08-4bcd-8c16-a775e2ea1f28', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-14 07:06:14.736328', '2025-07-14 07:06:14.736328', '681e93e4-7038-4c58-966d-5b0b8f3de4cd', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, '6f563bda-b8ec-4b6b-b28f-c594274d746a', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-14 07:13:30.796768', '2025-07-14 07:13:30.796768', '7721e2e8-a46a-43ee-b6de-6deca8825d12', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, '69da086b-dde7-4cd0-ab3d-6219d2927b6b', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-14 06:12:36.348291', '2025-07-14 06:12:36.348291', '773df2e2-d27f-4094-a5b4-83d959a1941a', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, '4101e168-c66c-4c1e-9512-241e761d0276', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-14 06:42:35.998653', '2025-07-14 06:42:35.998653', '782b1271-d03b-4fd5-8bc7-c5d636255c7a', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, 'f767713f-da9d-485c-8dff-c5464af34106', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-14 07:12:59.602862', '2025-07-14 07:12:59.602862', '8201f25a-287f-408e-a3aa-6ace09bbd9a2', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, '4eb4fa30-1168-4f40-9a27-9e25a85869f0', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-19 05:32:56.072467', '2025-07-19 05:32:56.072467', '89555969-b4bc-45cb-b4f5-c880b9e53f29', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, '867f5bb1-5f33-4d51-a8ce-cf8b8f33d228', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-14 07:19:06.469360', '2025-07-14 07:19:06.469360', '920d61a4-19cb-4ff6-b2a2-7f83a05b01db', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, '789159b0-a243-4ba4-8358-588f4be665ec', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-14 06:55:57.292736', '2025-07-14 06:55:57.292736', '95327099-aa85-4897-8748-91efa511e089', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, 'bd124dba-91fc-463e-b91b-588da8e7d9e2', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-14 06:04:11.320721', '2025-07-14 06:04:11.320721', '9fd8c835-db1c-496c-abd4-873f3fb50887', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, '7ee4583a-ca18-4e44-9354-bc42037f2c63', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-14 06:08:36.278340', '2025-07-14 06:08:36.278340', 'd82d2aa4-dfbd-42b4-92b1-0c3391384cc9', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, '9000e198-9fd6-4860-8eb9-e002b1180ddf', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-14 06:47:04.179244', '2025-07-14 06:47:04.179244', 'd9cca214-0fae-4032-a7b8-2e4071151aed', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, '5a47dde8-e195-4e4c-a8fe-7ca426e3934e', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-19 05:33:29.229884', '2025-07-19 05:33:29.229884', 'e3b8ce34-d539-4679-af5b-53146d189e38', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, 'cd0ff5fd-68b4-4c1c-b9d4-b6144f8e7b18', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-15 14:09:50.666280', '2025-07-15 14:09:50.666280', 'fba4c338-3ea1-4ae8-96bc-57826b745998', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, '8db6eaab-edb5-4b57-9b76-6aa1bca3161c', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');
INSERT INTO `workspace_user_roles` VALUES (NULL, '2025-07-19 08:05:12.097150', '2025-07-19 08:05:12.097150', 'ffee1493-014b-40f4-9342-7a7fef155a2f', NULL, NULL, '4bba3926-3e00-4aa5-87b6-c20eaa9448a1', NULL, '92588fe1-2c5b-4b96-a559-d9595da979d2', NULL, 'EMAIL', 'ACCEPTED', 'OWNER');

-- ----------------------------
-- Table structure for workspaces
-- ----------------------------
DROP TABLE IF EXISTS `workspaces`;
CREATE TABLE `workspaces`  (
  `id` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `public` tinyint(1) NOT NULL DEFAULT 0,
  `created_at` datetime(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
  `enable_ai` tinyint(1) NOT NULL DEFAULT 1,
  `enable_url_preview` tinyint(1) NOT NULL DEFAULT 0,
  `enable_doc_embedding` tinyint(1) NOT NULL DEFAULT 1,
  `name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `avatar_key` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `indexed` tinyint(1) NOT NULL DEFAULT 0,
  `created_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `updated_by` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  `sid` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL,
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of workspaces
-- ----------------------------
INSERT INTO `workspaces` VALUES ('10ed579e-43a6-4754-a708-5beab282ee95', 0, '2025-07-14 07:21:42.077', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('2d0366aa-f4ff-407d-8aaf-a0703b7d3b98', 0, '2025-07-20 03:14:55.558', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('4101e168-c66c-4c1e-9512-241e761d0276', 0, '2025-07-14 06:12:36.289', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('4eb4fa30-1168-4f40-9a27-9e25a85869f0', 0, '2025-07-14 07:12:59.534', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('5a47dde8-e195-4e4c-a8fe-7ca426e3934e', 0, '2025-07-14 06:47:04.167', 1, 0, 1, 'Test Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('5c647e19-61b3-4bdc-9e5b-a59878f1a6fd', 0, '2025-07-14 06:46:27.473', 1, 0, 1, 'Test Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('69da086b-dde7-4cd0-ab3d-6219d2927b6b', 0, '2025-07-14 07:13:30.779', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('6f563bda-b8ec-4b6b-b28f-c594274d746a', 0, '2025-07-14 07:06:14.681', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('789159b0-a243-4ba4-8358-588f4be665ec', 0, '2025-07-14 07:19:06.416', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('7ee4583a-ca18-4e44-9354-bc42037f2c63', 0, '2025-07-14 06:04:11.311', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('867f5bb1-5f33-4d51-a8ce-cf8b8f33d228', 0, '2025-07-19 05:32:55.994', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('8b84a46d-ffd8-4803-82ab-c2d3110ffdd6', 0, '2025-07-14 06:03:52.031', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('8db6eaab-edb5-4b57-9b76-6aa1bca3161c', 0, '2025-07-15 14:09:50.644', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('9000e198-9fd6-4860-8eb9-e002b1180ddf', 0, '2025-07-14 06:08:36.252', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('92588fe1-2c5b-4b96-a559-d9595da979d2', 0, '2025-07-19 08:05:12.064', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('991b6e05-87f8-4f70-848e-6e4d1215eef0', 0, '2025-07-19 05:44:10.560', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('b13f06ac-c685-4786-8fad-e121e8db3978', 0, '2025-07-14 06:08:26.799', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('bd124dba-91fc-463e-b91b-588da8e7d9e2', 0, '2025-07-14 06:55:57.281', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('cd0ff5fd-68b4-4c1c-b9d4-b6144f8e7b18', 0, '2025-07-19 05:33:29.218', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('d33eccd3-3d08-4bcd-8c16-a775e2ea1f28', 0, '2025-07-20 03:43:29.862', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('e41f86bb-e35f-408d-8c15-9775bde5c178', 0, '2025-07-16 05:41:22.354', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('e5068ed9-84a3-4701-a7be-93ca6a922f05', 0, '2025-07-14 05:47:26.647', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);
INSERT INTO `workspaces` VALUES ('f767713f-da9d-485c-8dff-c5464af34106', 0, '2025-07-14 06:42:35.965', 1, 0, 1, 'New Workspace', NULL, 0, NULL, NULL, NULL);

SET FOREIGN_KEY_CHECKS = 1;
