CREATE TABLE IF NOT EXISTS `sys_user_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` varchar(50) NOT NULL COMMENT '租户编码',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `effect` varchar(10) NOT NULL DEFAULT 'GRANT' COMMENT '覆盖效果：GRANT-额外允许，DENY-单独禁用',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_permission` (`tenant_code`,`user_id`,`permission_id`),
  KEY `idx_user_effect` (`tenant_code`,`user_id`,`effect`,`is_deleted`),
  KEY `idx_permission_id` (`permission_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户单独权限覆盖表';
