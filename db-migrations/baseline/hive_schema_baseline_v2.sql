
/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_auditor_candidate` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Tenant code',
  `approval_type` varchar(30) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Approval type: LEAVE/FINANCE/RESIGNATION',
  `approval_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Approval business code',
  `auditor_id` bigint NOT NULL COMMENT 'Candidate auditor user id',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '1 active, 2 closed',
  `audit_status` tinyint NOT NULL DEFAULT '0' COMMENT '0 pending, 1 approved, 2 rejected',
  `audit_comment` varchar(300) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'approver comment',
  `audit_time` datetime DEFAULT NULL COMMENT 'approver decision time',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  KEY `idx_approval_auditor_user` (`tenant_code`,`auditor_id`,`status`,`approval_type`),
  KEY `idx_approval_auditor_biz` (`tenant_code`,`approval_type`,`approval_code`,`status`),
  KEY `idx_approval_auditor_pending` (`tenant_code`,`approval_type`,`approval_code`,`status`,`audit_status`,`auditor_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Parallel approval auditor candidates';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `approval_default_auditor` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户编码',
  `approval_type` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '审批类型：ORDER/FINANCE/LEAVE/RESIGNATION',
  `auditor_id` bigint NOT NULL COMMENT '默认审批负责人用户ID',
  `auditor_ids` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT 'default approver id list, comma separated',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态：1启用 0停用',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_approval_default_tenant_type` (`tenant_code`,`approval_type`),
  KEY `idx_approval_default_auditor` (`tenant_code`,`auditor_id`,`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='审批默认负责人配置表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `punch_id` varchar(64) NOT NULL COMMENT '考勤主键：日期_用户ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tenant_code` varchar(50) NOT NULL COMMENT '租户编码',
  `sign_in_time` time DEFAULT NULL COMMENT '上班打卡时间',
  `sign_in_status` int DEFAULT NULL COMMENT '上班状态(0正常 1迟到等)',
  `sign_in_lat` double DEFAULT NULL COMMENT '上班打卡纬度',
  `sign_in_lng` double DEFAULT NULL COMMENT '上班打卡经度',
  `sign_in_distance` double DEFAULT NULL COMMENT '上班距离',
  `sign_in_address` varchar(255) DEFAULT NULL COMMENT '上班打卡地址',
  `sign_out_time` time DEFAULT NULL COMMENT '下班打卡时间',
  `sign_out_status` int DEFAULT NULL COMMENT '下班状态',
  `sign_out_lat` double DEFAULT NULL COMMENT '下班打卡纬度',
  `sign_out_lng` double DEFAULT NULL COMMENT '下班打卡经度',
  `sign_out_distance` double DEFAULT NULL COMMENT '下班距离',
  `sign_out_address` varchar(255) DEFAULT NULL COMMENT '下班打卡地址',
  `rule_radius` int DEFAULT NULL COMMENT '当天规则半径快照',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_punch_id` (`punch_id`),
  KEY `idx_tenant_code` (`tenant_code`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_attendance_record_tenant_punch_update` (`tenant_code`,`punch_id`,`update_time`,`id`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工打卡记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `attendance_statics` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `tenant_code` varchar(64) NOT NULL,
  `user_id` bigint NOT NULL COMMENT '??ID',
  `statistics_date` date NOT NULL,
  `expect_days` int DEFAULT '0',
  `actual_days` int DEFAULT '0',
  `late_count` int DEFAULT '0',
  `user_name` varchar(64) DEFAULT NULL COMMENT '???',
  `absent_count` int DEFAULT '0',
  `leave_early_count` int DEFAULT '0',
  `missing_count` int DEFAULT '0',
  `leave_days` decimal(10,2) DEFAULT '0.00',
  `abnormal_days` int DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_attendance_statics_user_date` (`tenant_code`,`user_id`,`statistics_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `bad_product_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `tenant_code` varchar(64) NOT NULL,
  `defective_id` varchar(64) NOT NULL,
  `order_id` varchar(64) DEFAULT NULL,
  `type` varchar(32) NOT NULL,
  `creator_id` bigint DEFAULT NULL COMMENT '???ID',
  `creator_name` varchar(64) DEFAULT NULL,
  `quantity` decimal(12,2) NOT NULL,
  `loss_amount` decimal(12,2) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `responsible_person` varchar(80) DEFAULT NULL COMMENT '负责人员',
  `process_measure` varchar(500) DEFAULT NULL COMMENT '处理措施',
  `improvement_plan` varchar(500) DEFAULT NULL COMMENT '改进方案',
  `attachment_name` varchar(180) DEFAULT NULL COMMENT '附件名称',
  `attachment_url` varchar(512) DEFAULT NULL COMMENT '附件地址',
  `attachment_size` bigint DEFAULT NULL COMMENT '附件大小，字节',
  `status` varchar(32) NOT NULL DEFAULT 'pending',
  `process_method` varchar(255) DEFAULT NULL,
  `process_remark` varchar(500) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_bad_product_defective_id` (`tenant_code`,`defective_id`),
  KEY `idx_bad_product_status` (`tenant_code`,`status`),
  KEY `idx_bad_product_type` (`tenant_code`,`type`),
  KEY `idx_bad_product_create_time` (`tenant_code`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cloth` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` varchar(50) NOT NULL COMMENT '租户编码',
  `barcode` varchar(100) NOT NULL COMMENT '布匹条码',
  `model_code` varchar(100) DEFAULT NULL COMMENT '型号编码',
  `spec` decimal(10,2) DEFAULT NULL COMMENT '规格',
  `meters` decimal(10,2) DEFAULT NULL COMMENT '初始米数',
  `status` int DEFAULT '0' COMMENT '状态(0-在库，1-已出库，2-部分出库)',
  `total_meters` decimal(10,2) DEFAULT NULL COMMENT '总米数',
  `remaining_meters` decimal(10,2) DEFAULT NULL COMMENT '剩余米数',
  `in_time` datetime DEFAULT NULL COMMENT '入库时间',
  `out_time` datetime DEFAULT NULL COMMENT '出库时间',
  `in_operator_id` bigint DEFAULT NULL COMMENT '入库操作人ID',
  `out_operator_id` bigint DEFAULT NULL COMMENT '出库操作人ID',
  `in_type` varchar(50) DEFAULT NULL COMMENT '入库类型',
  `is_bad` int DEFAULT '0' COMMENT '是否为次品(0-良品，1-次品)',
  `custom_fields_json` json DEFAULT NULL COMMENT '租户自定义库存字段',
  `version` int DEFAULT '0' COMMENT '乐观锁版本号',
  `del_flag` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标记',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_barcode` (`barcode`),
  KEY `idx_tenant_code` (`tenant_code`),
  KEY `idx_cloth_tenant_remaining` (`tenant_code`,`remaining_meters`),
  KEY `idx_cloth_tenant_model_remaining_update` (`tenant_code`,`model_code`,`remaining_meters`,`update_time`),
  KEY `idx_cloth_tenant_barcode` (`tenant_code`,`barcode`),
  KEY `idx_cloth_tenant_model_status` (`tenant_code`,`model_code`,`status`),
  KEY `idx_cloth_tenant_status_update` (`tenant_code`,`status`,`update_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2246 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='布匹表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `cloth_model_spec` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` varchar(50) NOT NULL COMMENT '租户编码',
  `model_code` varchar(100) NOT NULL COMMENT '型号编码',
  `spec` decimal(10,2) DEFAULT NULL COMMENT '规格',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_code` (`tenant_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2246 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='布匹型号规格字典表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `tenant_code` varchar(64) NOT NULL,
  `customer_name` varchar(128) NOT NULL,
  `customer_type` int DEFAULT NULL,
  `construction_area` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_customer_tenant` (`tenant_code`),
  KEY `idx_customer_tenant_name` (`tenant_code`,`customer_name`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer_contact` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `tenant_code` varchar(64) NOT NULL,
  `customer_id` bigint NOT NULL COMMENT '??ID',
  `contact_name` varchar(64) NOT NULL,
  `contact_phone` varchar(32) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_customer_contact_customer` (`customer_id`),
  KEY `idx_customer_contact_tenant_customer` (`tenant_code`,`customer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `customer_project` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `tenant_code` varchar(64) NOT NULL,
  `customer_id` bigint NOT NULL COMMENT '??ID',
  `project_name` varchar(128) NOT NULL,
  `construction_area` varchar(255) DEFAULT NULL COMMENT '施工区域',
  `project_owner` varchar(80) DEFAULT NULL COMMENT '项目负责人',
  PRIMARY KEY (`id`),
  KEY `idx_customer_project_customer` (`customer_id`),
  KEY `idx_customer_project_tenant_customer` (`tenant_code`,`customer_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `document` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `tenant_code` varchar(64) NOT NULL,
  `parent_id` bigint NOT NULL DEFAULT '0' COMMENT '??ID',
  `name` varchar(255) NOT NULL COMMENT '??',
  `original_name` varchar(255) DEFAULT NULL COMMENT 'Original uploaded filename',
  `type` int NOT NULL COMMENT '1-??? 2-??',
  `file_url` varchar(500) DEFAULT NULL,
  `storage_provider` varchar(32) DEFAULT NULL COMMENT 'Storage provider',
  `storage_bucket` varchar(128) DEFAULT NULL COMMENT 'Storage bucket name',
  `storage_object_key` varchar(700) DEFAULT NULL COMMENT 'Storage object key',
  `file_size` bigint DEFAULT NULL,
  `file_ext` varchar(32) DEFAULT NULL COMMENT '???',
  `mime_type` varchar(128) DEFAULT NULL,
  `file_hash` varchar(128) DEFAULT NULL COMMENT 'SHA-256 file hash',
  `etag` varchar(128) DEFAULT NULL COMMENT 'OSS ETag',
  `upload_status` varchar(32) DEFAULT NULL COMMENT 'Upload status',
  `creator_id` bigint DEFAULT NULL COMMENT '???ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_document_tenant_parent` (`tenant_code`,`parent_id`),
  KEY `idx_document_tenant_deleted` (`tenant_code`,`is_deleted`),
  KEY `idx_document_storage_key` (`tenant_code`,`storage_provider`,`storage_object_key`(191)),
  KEY `idx_document_storage_usage` (`tenant_code`,`is_deleted`,`type`,`file_size`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `emp_department` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(64) NOT NULL,
  `dept_name` varchar(64) NOT NULL,
  `dept_code` varchar(64) NOT NULL,
  `parent_id` bigint DEFAULT NULL COMMENT '上级部门ID',
  `leader_name` varchar(64) DEFAULT NULL COMMENT '部门负责人名称',
  `sort_no` int NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1',
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_department_code` (`tenant_code`,`dept_code`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='???';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `emp_employee_change_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(64) NOT NULL,
  `employee_id` bigint NOT NULL,
  `change_type` varchar(32) NOT NULL,
  `before_json` json DEFAULT NULL,
  `after_json` json DEFAULT NULL,
  `operator_user_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_emp_change_log_employee` (`employee_id`),
  KEY `idx_emp_change_log_tenant` (`tenant_code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `emp_employee_ext` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `tenant_code` varchar(64) NOT NULL,
  `emp_no` varchar(32) DEFAULT NULL,
  `email` varchar(128) DEFAULT NULL,
  `employee_type` varchar(32) DEFAULT NULL,
  `entry_date` date DEFAULT NULL,
  `avatar_url` varchar(255) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_employee_ext_user` (`user_id`),
  UNIQUE KEY `uk_emp_employee_ext_emp_no` (`tenant_code`,`emp_no`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `emp_position` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(64) NOT NULL,
  `position_name` varchar(64) NOT NULL,
  `position_code` varchar(64) NOT NULL,
  `department_id` bigint DEFAULT NULL,
  `sort_no` int NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1',
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_emp_position_code` (`tenant_code`,`position_code`),
  KEY `idx_emp_position_department` (`department_id`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='???';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee_attendance_location` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Tenant code',
  `user_id` bigint NOT NULL COMMENT 'Employee user id',
  `attendance_location_id` bigint NOT NULL COMMENT 'Attendance location id',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_employee_attendance_location` (`tenant_code`,`user_id`,`attendance_location_id`),
  KEY `idx_employee_attendance_location_user` (`tenant_code`,`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Employee assigned attendance locations';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `employee_resignation_approval` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `resignation_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '离职审批单号',
  `tenant_code` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户编码',
  `apply_user_id` bigint NOT NULL COMMENT '申请人ID',
  `expected_leave_date` date NOT NULL COMMENT '预计离职日期',
  `reason` varchar(500) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '离职原因',
  `handover_note` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '交接说明',
  `status` int NOT NULL DEFAULT '1' COMMENT '审批状态：1待审批 2已通过 3已拒绝',
  `auditor_id` bigint DEFAULT NULL COMMENT '当前审批人ID',
  `auditor_ids` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '当前候选审批人ID列表',
  `audit_comment` varchar(500) COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审批意见',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_resignation_code` (`resignation_code`),
  KEY `idx_resign_tenant_apply_status_time` (`tenant_code`,`apply_user_id`,`status`,`create_time`),
  KEY `idx_resign_tenant_auditor_status_time` (`tenant_code`,`auditor_id`,`status`,`create_time`),
  KEY `idx_resign_tenant_status_time` (`tenant_code`,`status`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工离职审批表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enterprise_announcement` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编码',
  `announcement_code` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告编号',
  `title` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告标题',
  `content` varchar(1000) COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告内容',
  `level` varchar(20) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'normal' COMMENT '公告级别(normal/urgent/important)',
  `route` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '前端跳转地址',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态(1有效，0失效)',
  `publisher_user_id` bigint DEFAULT NULL COMMENT '发布人用户ID',
  `publisher_name` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '发布人姓名',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enterprise_announcement_code` (`tenant_code`,`announcement_code`),
  KEY `idx_enterprise_announcement_recent` (`tenant_code`,`status`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='企业通知公告主表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `enterprise_announcement_read` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编码',
  `announcement_id` bigint NOT NULL COMMENT '公告ID',
  `announcement_code` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT '公告编号',
  `user_id` bigint NOT NULL COMMENT '员工用户ID',
  `user_name` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '员工姓名',
  `department_name` varchar(120) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '部门名称',
  `position_name` varchar(120) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '职位名称',
  `read_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否已读',
  `read_time` datetime DEFAULT NULL COMMENT '已读时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_enterprise_announcement_read_user` (`tenant_code`,`announcement_id`,`user_id`),
  KEY `idx_enterprise_announcement_read_code` (`tenant_code`,`announcement_code`,`user_id`),
  KEY `idx_enterprise_announcement_read_status` (`tenant_code`,`announcement_id`,`read_flag`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='企业通知公告已读记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `equipment_device` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户编码',
  `equipment_code` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备固定巡检码',
  `equipment_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备名称',
  `equipment_type` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备类型',
  `location` varchar(160) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '设备位置',
  `responsible_person` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '负责人',
  `inspection_cycle_days` int NOT NULL DEFAULT '7' COMMENT '巡检周期天数',
  `last_inspection_time` datetime DEFAULT NULL COMMENT '最近巡检时间',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'enabled' COMMENT '状态：enabled/disabled',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_equipment_tenant_code` (`tenant_code`,`equipment_code`),
  KEY `idx_equipment_tenant_status_update` (`tenant_code`,`status`,`update_time`),
  KEY `idx_equipment_tenant_name` (`tenant_code`,`equipment_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备档案表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `equipment_inspection_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '租户编码',
  `equipment_id` bigint NOT NULL COMMENT '设备ID',
  `equipment_code` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备固定巡检码',
  `equipment_name` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备名称快照',
  `inspection_result` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'normal' COMMENT '巡检结果：normal/abnormal',
  `abnormal_desc` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '异常说明',
  `photo_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '现场图片',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '备注',
  `inspector_user_id` bigint DEFAULT NULL COMMENT '巡检人用户ID',
  `inspector_name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '巡检人姓名',
  `inspection_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '巡检时间',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_equip_record_tenant_equipment_time` (`tenant_code`,`equipment_id`,`inspection_time`),
  KEY `idx_equip_record_tenant_code_time` (`tenant_code`,`equipment_code`,`inspection_time`),
  KEY `idx_equip_record_tenant_result_time` (`tenant_code`,`inspection_result`,`inspection_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='设备巡检记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `finance_approval` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `approval_code` varchar(64) NOT NULL,
  `tenant_code` varchar(64) NOT NULL,
  `apply_user_id` bigint NOT NULL COMMENT '???ID',
  `category` varchar(64) NOT NULL,
  `amount` decimal(12,2) NOT NULL,
  `reason` varchar(500) NOT NULL,
  `attachment_name` varchar(180) DEFAULT NULL COMMENT '附件名称',
  `attachment_url` varchar(512) DEFAULT NULL COMMENT '附件地址',
  `attachment_size` bigint DEFAULT NULL COMMENT '附件大小，字节',
  `status` tinyint NOT NULL DEFAULT '1',
  `auditor_id` bigint DEFAULT NULL,
  `auditor_ids` varchar(500) DEFAULT NULL COMMENT '当前候选审批人ID列表',
  `audit_comment` varchar(500) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_approval_code` (`tenant_code`,`approval_code`),
  KEY `idx_tenant_status_auditor` (`tenant_code`,`status`,`auditor_id`),
  KEY `idx_tenant_apply_user` (`tenant_code`,`apply_user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `installation_task` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `order_id` varchar(64) COLLATE utf8mb4_general_ci NOT NULL,
  `order_status` varchar(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'completed',
  `installation_status` varchar(40) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'production_completed',
  `customer_name` varchar(120) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `customer_phone` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `project_name` varchar(160) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `brand_name` varchar(120) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `order_category` varchar(40) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `goods_desc` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `total_quantity` int DEFAULT NULL,
  `information_channel` varchar(100) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `express_company` varchar(120) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `express_no` varchar(120) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `is_invoice` tinyint NOT NULL DEFAULT '0',
  `creator` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `order_attachment_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `order_attachment_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `order_attachment_size` bigint DEFAULT NULL,
  `construction_remark` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `special_exception_note` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `attachment_name` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `attachment_url` varchar(500) COLLATE utf8mb4_general_ci DEFAULT NULL,
  `attachment_size` bigint DEFAULT NULL,
  `order_completed_time` datetime DEFAULT NULL,
  `accepted_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_installation_task_order` (`tenant_code`,`order_id`),
  KEY `idx_installation_task_status` (`tenant_code`,`installation_status`,`update_time`),
  KEY `idx_installation_task_customer` (`tenant_code`,`customer_name`,`project_name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `installation_task_installer` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(64) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installation_task_id` bigint NOT NULL,
  `installer_name` varchar(50) COLLATE utf8mb4_unicode_ci NOT NULL,
  `installer_phone` varchar(40) COLLATE utf8mb4_unicode_ci NOT NULL,
  `sort_order` int NOT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_installation_task_installer_sort` (`tenant_code`,`installation_task_id`,`sort_order`),
  KEY `idx_installation_task_installer_task` (`tenant_code`,`installation_task_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` varchar(50) NOT NULL COMMENT '租户编码',
  `model_code` varchar(100) NOT NULL COMMENT '布匹条码',
  `cloth_id` bigint NOT NULL COMMENT '关联布匹ID',
  `operate_type` int NOT NULL COMMENT '操作类型：1-入库，2-出库',
  `operate_meters` decimal(10,2) NOT NULL COMMENT '操作米数',
  `operator_id` bigint NOT NULL COMMENT '操作人ID',
  `remark` varchar(255) DEFAULT NULL COMMENT '操作备注',
  `version` int DEFAULT '0' COMMENT '乐观锁版本号',
  `del_flag` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标记',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remaining_meters` decimal(10,2) NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_cloth_id` (`cloth_id`),
  KEY `idx_tenant_code` (`tenant_code`),
  KEY `idx_inventory_record_tenant_time` (`tenant_code`,`create_time`),
  KEY `idx_inventory_record_tenant_type_time` (`tenant_code`,`operate_type`,`create_time`),
  KEY `idx_inventory_record_tenant_model_time` (`tenant_code`,`model_code`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2509 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存出入记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory_setting` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Tenant code',
  `warning_threshold_meters` decimal(18,2) NOT NULL DEFAULT '100.00' COMMENT 'Inventory low-stock warning threshold in meters',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inventory_setting_tenant` (`tenant_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Inventory warning settings';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory_statics` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `stat_date` datetime NOT NULL COMMENT '统计日期（核心维度）',
  `total_roll_count` bigint DEFAULT '0' COMMENT '总卷数',
  `total_meters` decimal(12,2) DEFAULT '0.00' COMMENT '总米数',
  `day_in_meters` decimal(12,2) DEFAULT '0.00' COMMENT '日入米数',
  `day_out_meters` decimal(12,2) DEFAULT '0.00' COMMENT '日出米数',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `tenant_code` varchar(255) NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_inventory_statics_tenant_date` (`tenant_code`,`stat_date`),
  KEY `idx_inventory_statics_tenant_stat_date` (`tenant_code`,`stat_date`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='库存每日统计表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `label_template` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `tenant_code` varchar(64) NOT NULL,
  `name` varchar(100) NOT NULL,
  `print_type` varchar(32) NOT NULL DEFAULT 'label',
  `content` longtext NOT NULL,
  `design_json` json DEFAULT NULL COMMENT 'visual designer json',
  `width_mm` decimal(10,2) NOT NULL DEFAULT '70.00' COMMENT 'label width mm',
  `height_mm` decimal(10,2) NOT NULL DEFAULT '50.00' COMMENT 'label height mm',
  `variables` varchar(500) DEFAULT NULL,
  `file_name` varchar(255) DEFAULT NULL,
  `file_size` bigint DEFAULT NULL,
  `is_default` tinyint NOT NULL DEFAULT '0',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '???0-???1-??',
  `creator_id` bigint DEFAULT NULL COMMENT '???ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_type_status` (`tenant_code`,`print_type`,`status`),
  KEY `idx_tenant_default` (`tenant_code`,`print_type`,`is_default`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `notification_record` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编码',
  `dedupe_key` varchar(220) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务去重键',
  `biz_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '业务类型',
  `biz_id` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '业务ID',
  `title` varchar(120) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '通知标题',
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '通知内容',
  `level` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'info' COMMENT '级别(info/warning/critical)',
  `channel` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'IN_APP' COMMENT '通知渠道',
  `route` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '前端跳转地址',
  `receiver_user_id` bigint DEFAULT NULL COMMENT '接收人用户ID，空表示租户广播',
  `receiver_name` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '接收人姓名',
  `receiver_phone` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '接收人手机号',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '状态(1有效，0失效)',
  `read_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否已读',
  `read_time` datetime DEFAULT NULL COMMENT '已读时间',
  `send_status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT '外部推送状态',
  `send_time` datetime DEFAULT NULL COMMENT '外部推送时间',
  `task_status` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'PENDING' COMMENT '待办状态(PENDING处理中，DONE已处理，IGNORED暂不处理)',
  `close_result` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '闭环结果(resolved/ignored)',
  `close_note` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '闭环备注',
  `close_user_id` bigint DEFAULT NULL COMMENT '闭环处理人ID',
  `close_time` datetime DEFAULT NULL COMMENT '闭环处理时间',
  `source_type` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'system' COMMENT '来源类型',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_dedupe` (`tenant_code`,`dedupe_key`),
  KEY `idx_notification_unread` (`tenant_code`,`receiver_user_id`,`read_flag`,`status`,`update_time`),
  KEY `idx_notification_task_status` (`tenant_code`,`task_status`,`update_time`),
  KEY `idx_notification_announcement_receiver` (`tenant_code`,`biz_type`,`biz_id`,`receiver_user_id`,`read_flag`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='通知提醒记录表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `trace_id` varchar(64) NOT NULL COMMENT '链路追踪ID',
  `tenant_code` varchar(64) DEFAULT NULL COMMENT '租户编码',
  `user_id` bigint DEFAULT NULL COMMENT '操作用户ID',
  `module` varchar(64) NOT NULL COMMENT '业务模块',
  `action` varchar(64) NOT NULL COMMENT '操作动作',
  `biz_type` varchar(64) DEFAULT NULL COMMENT '业务类型',
  `biz_no` varchar(128) DEFAULT NULL COMMENT '业务编号',
  `description` varchar(255) DEFAULT NULL COMMENT '操作说明',
  `log_level` varchar(16) NOT NULL DEFAULT 'INFO' COMMENT '日志级别：INFO-信息，WARN-警告，ERROR-错误',
  `class_name` varchar(255) DEFAULT NULL COMMENT '类名',
  `method_name` varchar(128) DEFAULT NULL COMMENT '方法名',
  `request_method` varchar(16) DEFAULT NULL COMMENT '请求方法',
  `request_uri` varchar(500) DEFAULT NULL COMMENT '请求地址',
  `client_ip` varchar(64) DEFAULT NULL COMMENT '客户端IP',
  `user_agent` varchar(500) DEFAULT NULL COMMENT '客户端标识',
  `args_json` text COMMENT '脱敏后的入参',
  `result_json` text COMMENT '脱敏后的返回值',
  `success` tinyint NOT NULL DEFAULT '1' COMMENT '是否成功：0-失败，1-成功',
  `slow` tinyint NOT NULL DEFAULT '0' COMMENT '是否慢操作：0-否，1-是',
  `duration_ms` bigint NOT NULL DEFAULT '0' COMMENT '耗时毫秒',
  `error_type` varchar(255) DEFAULT NULL COMMENT '异常类型',
  `error_message` text COMMENT '异常信息',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_trace_id` (`trace_id`),
  KEY `idx_tenant_time` (`tenant_code`,`create_time`),
  KEY `idx_level_time` (`log_level`,`create_time`),
  KEY `idx_module_action_time` (`module`,`action`,`create_time`),
  KEY `idx_biz` (`biz_type`,`biz_no`),
  KEY `idx_operation_log_tenant_level_time` (`tenant_code`,`log_level`,`create_time`),
  KEY `idx_operation_log_success_time` (`success`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='公共操作日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_setting` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Tenant code',
  `stale_warning_days` int NOT NULL DEFAULT '3' COMMENT 'Warn when an active order has not been updated for N days',
  `sample_room_stale_warning_days` int NOT NULL DEFAULT '3' COMMENT '样板间订单未更新预警天数',
  `bulk_stale_warning_days` int NOT NULL DEFAULT '3' COMMENT '大货订单未更新预警天数',
  `replenishment_stale_warning_days` int NOT NULL DEFAULT '3' COMMENT '补单订单未更新预警天数',
  `drawing_budget_stale_warning_days` int NOT NULL DEFAULT '3' COMMENT '图纸预算订单未更新预警天数',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_setting_tenant` (`tenant_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Order warning settings';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `outbound_item` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `tenant_code` varchar(64) NOT NULL,
  `order_id` bigint NOT NULL COMMENT '???ID',
  `barcode` varchar(128) NOT NULL COMMENT '??',
  `model_code` varchar(128) DEFAULT NULL,
  `spec` decimal(10,2) DEFAULT NULL COMMENT '??',
  `meters` decimal(10,2) DEFAULT NULL,
  `price` decimal(12,2) DEFAULT NULL COMMENT '??',
  `total_amount` decimal(12,2) DEFAULT NULL COMMENT '???',
  `remark` varchar(255) DEFAULT NULL COMMENT 'Line remark shown on printed receipt',
  `request_id` varchar(64) DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_outbound_item_request_id` (`tenant_code`,`request_id`),
  KEY `idx_outbound_item_order` (`order_id`),
  KEY `idx_outbound_item_tenant_barcode` (`tenant_code`,`barcode`),
  KEY `idx_outbound_item_tenant_order` (`tenant_code`,`order_id`),
  KEY `idx_outbound_item_request` (`request_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `outbound_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `tenant_code` varchar(64) NOT NULL,
  `order_no` varchar(64) NOT NULL,
  `biz_order_no` varchar(64) DEFAULT NULL,
  `customer_name` varchar(128) DEFAULT NULL,
  `project_name` varchar(128) DEFAULT NULL COMMENT 'Project name shown on printed receipt',
  `print_date` date DEFAULT NULL COMMENT 'Date shown on printed receipt',
  `logistics_company` varchar(64) DEFAULT NULL COMMENT 'Logistics company shown on printed receipt',
  `logistics_no` varchar(128) DEFAULT NULL COMMENT 'Logistics number shown on printed receipt',
  `print_operator_name` varchar(64) DEFAULT NULL COMMENT 'Operator name shown on printed receipt',
  `print_edit_count` int NOT NULL DEFAULT '0' COMMENT 'Manual print edit count',
  `order_status` tinyint NOT NULL DEFAULT '0' COMMENT '0-??? 1-??? 2-??? 3-??',
  `print_status` tinyint NOT NULL DEFAULT '0' COMMENT '0-??? 1-???',
  `operator_id` bigint DEFAULT NULL COMMENT '???ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_outbound_order_no` (`tenant_code`,`order_no`),
  KEY `idx_outbound_print_queue` (`tenant_code`,`order_status`,`print_status`,`update_time`),
  KEY `idx_outbound_biz_order` (`tenant_code`,`biz_order_no`,`print_status`),
  KEY `idx_outbound_order_tenant_print_status` (`tenant_code`,`print_status`,`order_status`,`create_time`),
  KEY `idx_outbound_order_tenant_biz` (`tenant_code`,`biz_order_no`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `outbound_print_edit_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Tenant code',
  `order_id` bigint NOT NULL COMMENT 'Outbound order id',
  `order_no_before` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Order number before edit',
  `order_no_after` varchar(64) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Order number after edit',
  `operator_user_id` bigint DEFAULT NULL COMMENT 'Operator user id',
  `before_json` json DEFAULT NULL COMMENT 'Before edit snapshot',
  `after_json` json DEFAULT NULL COMMENT 'After edit snapshot',
  `edit_reason` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Edit reason',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  PRIMARY KEY (`id`),
  KEY `idx_outbound_print_edit_order` (`tenant_code`,`order_id`,`create_time`),
  KEY `idx_outbound_print_edit_order_no` (`tenant_code`,`order_no_after`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Outbound print edit audit log';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `price_change_log` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(64) NOT NULL,
  `sku_id` bigint NOT NULL,
  `model_code` varchar(128) NOT NULL,
  `old_price` decimal(12,2) DEFAULT NULL,
  `new_price` decimal(12,2) NOT NULL,
  `operator_user_id` bigint DEFAULT NULL,
  `remark` varchar(255) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_price_log_sku` (`tenant_code`,`sku_id`),
  KEY `idx_price_log_model` (`tenant_code`,`model_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `price_customer_override` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(64) NOT NULL,
  `sku_id` bigint NOT NULL,
  `customer_id` bigint NOT NULL,
  `customer_name` varchar(128) DEFAULT NULL,
  `price` decimal(12,2) NOT NULL,
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_price_override` (`tenant_code`,`sku_id`,`customer_id`),
  KEY `idx_price_override_customer` (`tenant_code`,`customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `price_sku` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(64) NOT NULL,
  `model_code` varchar(128) NOT NULL,
  `batch_no` varchar(64) DEFAULT NULL COMMENT '??',
  `category` varchar(64) DEFAULT NULL,
  `spec` varchar(255) DEFAULT NULL,
  `base_price` decimal(12,2) NOT NULL DEFAULT '0.00' COMMENT '???',
  `currency` varchar(16) NOT NULL DEFAULT 'CNY' COMMENT '??',
  `effective_date` date NOT NULL,
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '0-??? 1-??? 2-???',
  `image_url` varchar(500) DEFAULT NULL,
  `remark` varchar(500) DEFAULT NULL COMMENT '??',
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_price_sku_model` (`tenant_code`,`model_code`),
  KEY `idx_price_sku_status` (`tenant_code`,`status`),
  KEY `idx_price_sku_effective` (`tenant_code`,`effective_date`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='??SKU?';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `price_tier_price` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(64) NOT NULL,
  `sku_id` bigint NOT NULL,
  `tier_code` varchar(32) NOT NULL,
  `tier_name` varchar(64) NOT NULL,
  `fixed_price` decimal(12,2) DEFAULT NULL COMMENT '???',
  `discount_rate` decimal(8,2) DEFAULT NULL COMMENT '???',
  `is_deleted` tinyint NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_price_tier` (`tenant_code`,`sku_id`,`tier_code`),
  KEY `idx_price_tier_sku` (`sku_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `print_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `tenant_code` varchar(50) NOT NULL,
  `task_no` varchar(64) NOT NULL,
  `print_type` varchar(32) NOT NULL,
  `biz_type` varchar(64) DEFAULT NULL,
  `biz_no` varchar(100) DEFAULT NULL,
  `source_order_no` varchar(100) DEFAULT NULL,
  `template_id` bigint DEFAULT NULL COMMENT '??ID',
  `template_name` varchar(100) DEFAULT NULL,
  `print_channel` varchar(32) DEFAULT NULL,
  `device_name` varchar(100) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT '0' COMMENT '??(0-???,1-???,2-??,3-??)',
  `retry_count` int NOT NULL DEFAULT '0',
  `max_retry` int NOT NULL DEFAULT '3',
  `print_payload_json` json DEFAULT NULL,
  `error_message` varchar(500) DEFAULT NULL,
  `operator_id` bigint DEFAULT NULL COMMENT '???ID',
  `printed_time` datetime DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_print_task_no` (`task_no`),
  KEY `idx_print_task_tenant_status` (`tenant_code`,`status`,`create_time`),
  KEY `idx_print_task_biz` (`tenant_code`,`print_type`,`biz_no`),
  KEY `idx_print_task_create_time` (`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `production_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` varchar(50) NOT NULL COMMENT '租户编码',
  `order_id` varchar(50) NOT NULL COMMENT '生产订单编号',
  `sales_order_id` varchar(50) DEFAULT NULL,
  `status` varchar(50) DEFAULT NULL COMMENT '订单状态',
  `order_category` varchar(32) NOT NULL DEFAULT 'bulk' COMMENT '订单小项：sample_room-样板间，bulk-大货，replenishment-补单',
  `model` varchar(100) DEFAULT NULL COMMENT '面料型号',
  `fabric` varchar(100) DEFAULT NULL COMMENT '面料名称',
  `weight` decimal(10,2) DEFAULT NULL COMMENT '克重',
  `width` decimal(10,2) DEFAULT NULL COMMENT '幅宽(cm)',
  `color` varchar(50) DEFAULT NULL COMMENT '颜色',
  `quantity` int DEFAULT NULL COMMENT '数量',
  `price` decimal(12,2) DEFAULT NULL COMMENT '单价(元)',
  `total_amount` decimal(12,2) DEFAULT NULL COMMENT '总价',
  `process` int DEFAULT NULL COMMENT '当前生产工序(0-4)',
  `customer_id` varchar(50) DEFAULT NULL COMMENT '客户ID',
  `customer_name` varchar(100) DEFAULT NULL COMMENT '客户名称',
  `project_name` varchar(100) DEFAULT NULL COMMENT '项目名称',
  `brand_name` varchar(120) DEFAULT NULL COMMENT '品牌',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `information_channel` varchar(100) DEFAULT NULL COMMENT '信息渠道',
  `creator` varchar(50) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(50) DEFAULT NULL COMMENT '更新人',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_id` (`order_id`),
  KEY `idx_tenant_code` (`tenant_code`),
  KEY `idx_production_order_tenant_create_time` (`tenant_code`,`create_time`),
  KEY `idx_production_order_tenant_status_update` (`tenant_code`,`status`,`update_time`),
  KEY `idx_production_order_tenant_sales` (`tenant_code`,`sales_order_id`),
  KEY `idx_production_order_tenant_brand` (`tenant_code`,`brand_name`),
  KEY `idx_production_order_tenant_category_status` (`tenant_code`,`order_category`,`status`,`update_time`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产订单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `production_order_status_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` varchar(50) NOT NULL COMMENT '租户编码',
  `order_id` varchar(50) NOT NULL COMMENT '生产订单编号',
  `old_status` varchar(50) DEFAULT NULL COMMENT '变更前状态',
  `new_status` varchar(50) DEFAULT NULL COMMENT '变更后状态',
  `operate_type` varchar(50) DEFAULT NULL COMMENT '操作类型：manual/scan/auto',
  `remark` varchar(255) DEFAULT NULL COMMENT '变更备注',
  `operator` varchar(50) DEFAULT NULL COMMENT '操作人',
  `operator_name` varchar(100) DEFAULT NULL COMMENT '操作人姓名',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`),
  KEY `idx_order_id` (`order_id`),
  KEY `idx_tenant_order_time` (`tenant_code`,`order_id`,`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生产订单状态变更日志表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sales_order` (
  `order_id` varchar(50) NOT NULL COMMENT '订单ID（主键，手动生成）',
  `tenant_code` varchar(50) NOT NULL COMMENT '租户编码',
  `status` varchar(50) DEFAULT NULL COMMENT '订单状态',
  `order_category` varchar(32) NOT NULL DEFAULT 'bulk' COMMENT '订单小项：sample_room-样板间，bulk-大货，replenishment-补单',
  `customer_name` varchar(100) DEFAULT NULL COMMENT '客户名称',
  `customer_phone` varchar(20) DEFAULT NULL COMMENT '客户联系方式',
  `project_name` varchar(100) DEFAULT NULL,
  `brand_name` varchar(120) DEFAULT NULL COMMENT '品牌',
  `goods_desc` varchar(500) DEFAULT NULL COMMENT '商品描述',
  `total_amount` decimal(12,2) DEFAULT NULL COMMENT '订单总金额',
  `total_quantity` int DEFAULT NULL COMMENT '订单总数量',
  `information_channel` varchar(100) DEFAULT NULL COMMENT '信息渠道',
  `cancel_reason` varchar(500) DEFAULT NULL COMMENT '取消原因',
  `express_company` varchar(100) DEFAULT NULL COMMENT '物流公司',
  `express_no` varchar(100) DEFAULT NULL COMMENT '物流单号',
  `creator` varchar(50) DEFAULT NULL COMMENT '创建人',
  `updater` varchar(50) DEFAULT NULL COMMENT '更新人',
  `attachment_name` varchar(255) DEFAULT NULL COMMENT '附件原始文件名',
  `attachment_url` varchar(500) DEFAULT NULL COMMENT '附件访问地址',
  `attachment_size` bigint DEFAULT NULL COMMENT '附件大小，单位字节',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_invoice` tinyint NOT NULL DEFAULT '0' COMMENT '是否开票：0-否，1-是',
  PRIMARY KEY (`order_id`),
  KEY `idx_tenant_code` (`tenant_code`),
  KEY `idx_sales_order_tenant_create_time` (`tenant_code`,`create_time`),
  KEY `idx_sales_order_tenant_status_update` (`tenant_code`,`status`,`update_time`),
  KEY `idx_sales_order_tenant_customer` (`tenant_code`,`customer_name`),
  KEY `idx_sales_order_tenant_customer_time` (`tenant_code`,`customer_name`,`create_time`),
  KEY `idx_sales_order_tenant_brand` (`tenant_code`,`brand_name`),
  KEY `idx_sales_order_tenant_category_status` (`tenant_code`,`order_category`,`status`,`update_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='销售订单表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sales_order_detail` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `order_id` varchar(50) NOT NULL,
  `tenant_code` varchar(50) NOT NULL,
  `model_code` varchar(128) DEFAULT NULL,
  `weight` varchar(64) DEFAULT NULL COMMENT '商品类别',
  `spec` varchar(128) DEFAULT NULL COMMENT '??',
  `quantity` decimal(12,2) DEFAULT NULL COMMENT '??',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_sales_order_detail_order` (`order_id`),
  KEY `idx_sales_order_detail_tenant_order` (`tenant_code`,`order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sales_order_note` (
  `id` bigint NOT NULL,
  `tenant_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `order_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `content` varchar(1000) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `creator_user_id` bigint NOT NULL,
  `creator_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `updater_user_id` bigint NOT NULL,
  `updater_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
  `version` int NOT NULL DEFAULT '0',
  `create_time` datetime NOT NULL,
  `update_time` datetime NOT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_sales_order_note_order` (`tenant_code`,`order_id`,`update_time`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sales_order_status_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` varchar(50) NOT NULL COMMENT '租户编码',
  `order_id` varchar(64) NOT NULL COMMENT '销售订单号',
  `old_status` varchar(64) DEFAULT NULL COMMENT '变更前状态',
  `new_status` varchar(64) DEFAULT NULL COMMENT '变更后状态',
  `operate_type` varchar(32) DEFAULT NULL COMMENT '操作类型：create-创建，status_change-状态变更，sync-关联同步，update-信息更新',
  `remark` varchar(500) DEFAULT NULL COMMENT '操作备注',
  `operator` varchar(64) DEFAULT NULL COMMENT '操作人',
  `operator_name` varchar(100) DEFAULT NULL COMMENT '操作人姓名',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_sales_order_status_log_tenant_order` (`tenant_code`,`order_id`),
  KEY `idx_sales_order_status_log_create_time` (`create_time`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='销售订单状态流转日志';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `schema_migration_history` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `version` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Migration version',
  `file_name` varchar(255) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Migration file',
  `checksum_sha256` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Migration SHA256',
  `status` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'RUNNING/SUCCESS/FAILED',
  `error_message` text COLLATE utf8mb4_general_ci COMMENT 'Failure reason',
  `executed_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'First execution time',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Last update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_schema_migration_version` (`version`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Database migration history';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '权限主键ID',
  `parent_id` bigint DEFAULT '0' COMMENT '父级权限ID（用于前端构建菜单树，顶级菜单为0）',
  `perm_code` varchar(100) NOT NULL COMMENT '权限编码（全局唯一）',
  `module_code` varchar(64) DEFAULT NULL COMMENT '权限所属模块',
  `perm_type` int DEFAULT NULL COMMENT '类型：1-目录，2-菜单，3-按钮/接口',
  `assignable` tinyint NOT NULL DEFAULT '0' COMMENT '是否可分配：0-否，1-是',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '权限状态：0-停用，1-启用',
  `sort` int DEFAULT '0' COMMENT '排序号',
  `perm_name` varchar(100) NOT NULL COMMENT '权限名称',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_perm_code` (`perm_code`)
) ENGINE=InnoDB AUTO_INCREMENT=2148 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统权限表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_permission_catalog` (
  `id` tinyint NOT NULL,
  `catalog_version` bigint NOT NULL DEFAULT '1',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户权限目录版本';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '角色主键ID',
  `tenant_code` varchar(50) NOT NULL COMMENT '租户编码',
  `role_code` varchar(50) NOT NULL COMMENT '角色编码',
  `role_name` varchar(50) NOT NULL COMMENT '角色名称',
  `is_system` int DEFAULT '0' COMMENT '是否内置角色：1-是 0-否',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识',
  PRIMARY KEY (`id`),
  KEY `idx_tenant_code` (`tenant_code`)
) ENGINE=InnoDB AUTO_INCREMENT=74 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='系统角色表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_role_permission` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `permission_id` bigint NOT NULL COMMENT '权限ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `is_deleted` tinyint(1) DEFAULT '0',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_role_perm` (`role_id`,`permission_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2639 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='角色权限关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_permission` (
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
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `sys_user_role` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `tenant_code` varchar(50) NOT NULL COMMENT '租户编码',
  `role_id` bigint NOT NULL COMMENT '角色ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `is_deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识',
  PRIMARY KEY (`id`),
  KEY `idx_user_tenant` (`user_id`,`tenant_code`)
) ENGINE=InnoDB AUTO_INCREMENT=17 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户角色关联表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `system_event` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `event_key` varchar(120) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Event unique key',
  `source_app` varchar(80) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Source application',
  `event_type` varchar(80) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Event type',
  `level` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'INFO/WARN/ERROR',
  `tenant_code` varchar(50) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Tenant code',
  `module` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Module name',
  `title` varchar(180) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Event title',
  `content` varchar(1000) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Event content',
  `biz_type` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Business type',
  `biz_no` varchar(160) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Business number',
  `trace_id` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Trace id',
  `detail_json` json DEFAULT NULL COMMENT 'Structured event detail',
  `handled` tinyint NOT NULL DEFAULT '0' COMMENT '0=unhandled, 1=handled',
  `handled_by` varchar(80) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Handler',
  `handled_time` datetime DEFAULT NULL COMMENT 'Handled time',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  PRIMARY KEY (`id`),
  KEY `idx_system_event_level_time` (`level`,`create_time`),
  KEY `idx_system_event_type_time` (`event_type`,`create_time`),
  KEY `idx_system_event_source_time` (`source_app`,`create_time`),
  KEY `idx_system_event_tenant_time` (`tenant_code`,`create_time`),
  KEY `idx_system_event_handled` (`handled`,`level`,`create_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='System important event table';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '租户ID',
  `tenant_code` varchar(50) NOT NULL COMMENT '租户编码',
  `tenant_name` varchar(100) NOT NULL COMMENT '租户名称',
  `logo_url` varchar(500) DEFAULT NULL COMMENT '公司Logo地址',
  `tenant_type` int DEFAULT '1' COMMENT '租户类型：1-普通租户，2-vip租户，3-超级租户',
  `contact_person` varchar(50) DEFAULT NULL COMMENT '联系人',
  `contact_phone` varchar(20) DEFAULT NULL COMMENT '联系电话',
  `password` varchar(255) DEFAULT NULL COMMENT '租户登录密码',
  `status` int DEFAULT '1' COMMENT '租户状态：0-禁用，1-启用，2-冻结，3-待审核',
  `package_code` varchar(50) DEFAULT 'TRIAL' COMMENT 'Commercial package code',
  `package_name` varchar(100) DEFAULT '试用版' COMMENT 'Commercial package name',
  `subscription_status` varchar(50) DEFAULT 'TRIAL' COMMENT 'Subscription status: TRIAL/ACTIVE/EXPIRED/SUSPENDED',
  `subscription_start_time` datetime DEFAULT NULL COMMENT 'Subscription start time',
  `subscription_end_time` datetime DEFAULT NULL COMMENT 'Subscription end time',
  `max_users` int DEFAULT '5' COMMENT 'Maximum enabled employees, 0 means blocked',
  `max_storage_mb` int DEFAULT '512' COMMENT 'Storage quota in MB',
  `feature_flags` json DEFAULT NULL COMMENT 'Tenant feature flags',
  `creator` bigint DEFAULT NULL COMMENT '创建人ID',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT '0' COMMENT '逻辑删除标识：0-未删除，1-已删除',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_code` (`tenant_code`),
  KEY `idx_tenant_subscription` (`subscription_status`,`subscription_end_time`),
  KEY `idx_tenant_package` (`package_code`,`status`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_attendance_location` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Tenant code',
  `location_name` varchar(100) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'Company punch location' COMMENT 'Location name',
  `latitude` double NOT NULL COMMENT 'Latitude',
  `longitude` double NOT NULL COMMENT 'Longitude',
  `address` varchar(255) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT 'Address',
  `radius` double NOT NULL DEFAULT '300' COMMENT 'Allowed radius in meters',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT 'Status: 1 enabled, 0 disabled',
  `sort_order` int NOT NULL DEFAULT '0' COMMENT 'Sort order',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  KEY `idx_attendance_location_tenant_status` (`tenant_code`,`status`,`sort_order`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Attendance punch locations';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_attendance_rule` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` varchar(50) NOT NULL COMMENT '租户编码',
  `tenant_name` varchar(100) DEFAULT NULL COMMENT '租户名称',
  `status` int DEFAULT '0' COMMENT '状态',
  `latitude` double DEFAULT NULL COMMENT '打卡纬度',
  `longitude` double DEFAULT NULL COMMENT '打卡经度',
  `address` varchar(255) DEFAULT NULL COMMENT '打卡地址',
  `radius` int DEFAULT NULL COMMENT '有效打卡半径',
  `work_start_time` time DEFAULT NULL COMMENT '上班时间',
  `work_end_time` time DEFAULT NULL COMMENT '下班时间',
  `off_work_start_time` time DEFAULT NULL COMMENT '允许早退打卡时间',
  `off_work_end_time` time DEFAULT NULL COMMENT '最晚下班打卡时间',
  `over_time_start_time` time DEFAULT NULL COMMENT '加班开始计算时间',
  `over_time_end_time` time DEFAULT NULL COMMENT '最晚加班打卡时间',
  `late_tolerance_minutes` int DEFAULT '0',
  `early_tolerance_minutes` int DEFAULT '0',
  `work_days` varchar(32) DEFAULT '1,2,3,4,5',
  `enable_gps` tinyint DEFAULT '1',
  `enable_wifi` tinyint DEFAULT '0',
  `wifi_ssid` varchar(128) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_tenant_code` (`tenant_code`)
) ENGINE=InnoDB AUTO_INCREMENT=6 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='租户考勤规则表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_field_config` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户编码',
  `module_code` varchar(64) COLLATE utf8mb4_general_ci NOT NULL COMMENT '模块编码',
  `field_key` varchar(80) COLLATE utf8mb4_general_ci NOT NULL COMMENT '字段编码',
  `field_label` varchar(80) COLLATE utf8mb4_general_ci NOT NULL COMMENT '租户侧字段名称',
  `visible_flag` tinyint NOT NULL DEFAULT '1' COMMENT '是否显示：0否 1是',
  `required_flag` tinyint NOT NULL DEFAULT '0' COMMENT '是否必填：0否 1是',
  `sort_no` int NOT NULL DEFAULT '0' COMMENT '排序号',
  `field_type` varchar(30) COLLATE utf8mb4_general_ci NOT NULL DEFAULT 'text' COMMENT '字段类型：text/number/date/datetime/select',
  `options_json` json DEFAULT NULL COMMENT '字段选项配置',
  `remark` varchar(300) COLLATE utf8mb4_general_ci DEFAULT NULL COMMENT '配置备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_field_config` (`tenant_code`,`module_code`,`field_key`),
  KEY `idx_tenant_field_config_module` (`tenant_code`,`module_code`,`sort_no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='租户字段级定制配置';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_manual` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `tenant_code` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci,
  `updater_id` bigint DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_manual_tenant` (`tenant_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='企业自定义使用手册';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `tenant_usage_meter` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
  `tenant_code` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Tenant code',
  `meter_type` varchar(50) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Usage meter type',
  `period_key` varchar(20) COLLATE utf8mb4_general_ci NOT NULL COMMENT 'Billing period, e.g. 202605',
  `used_count` int NOT NULL DEFAULT '0' COMMENT 'Used count in this period',
  `limit_count` int DEFAULT NULL COMMENT 'Configured limit in this period',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_usage_meter` (`tenant_code`,`meter_type`,`period_key`),
  KEY `idx_tenant_usage_period` (`meter_type`,`period_key`,`used_count`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci COMMENT='Tenant commercial usage meter';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `tenant_code` varchar(50) DEFAULT NULL COMMENT '租户编码，微信一键登录未加入组织时为空',
  `name` varchar(50) DEFAULT NULL COMMENT '姓名',
  `login_name` varchar(64) DEFAULT NULL,
  `phone` varchar(20) DEFAULT NULL COMMENT '手机号',
  `password` varchar(128) DEFAULT NULL,
  `must_change_password` tinyint NOT NULL DEFAULT '0' COMMENT '首次登录是否必须修改密码',
  `department_name` varchar(100) DEFAULT NULL COMMENT '部门名称',
  `position` varchar(50) DEFAULT NULL COMMENT '工作岗位',
  `manager_id` bigint DEFAULT NULL COMMENT '直属上级ID',
  `manager_name` varchar(64) DEFAULT NULL COMMENT '直属主管姓名',
  `status` tinyint NOT NULL DEFAULT '1' COMMENT '???0-?? 1-?? 2-??',
  `attendance_required` tinyint NOT NULL DEFAULT '1' COMMENT 'Attendance required: 1 yes, 0 no',
  `role_level` int DEFAULT NULL COMMENT '角色等级',
  `permission_version` bigint NOT NULL DEFAULT '1' COMMENT '权限版本',
  `auth_version` bigint NOT NULL DEFAULT '1' COMMENT '登录态版本',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `phone_hash` varchar(128) DEFAULT NULL COMMENT '手机号不可逆哈希',
  `phone_mask` varchar(32) DEFAULT NULL COMMENT '手机号脱敏展示',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_tenant_login_name` (`tenant_code`,`login_name`),
  KEY `idx_tenant_code` (`tenant_code`),
  KEY `idx_user_tenant_phone_hash` (`tenant_code`,`phone_hash`),
  KEY `idx_user_phone_hash_status` (`phone_hash`,`status`,`id`),
  KEY `idx_user_attendance_required` (`tenant_code`,`status`,`attendance_required`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='用户表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `user_leave` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `leave_code` varchar(50) NOT NULL COMMENT '请假编码',
  `tenant_code` varchar(50) NOT NULL COMMENT '租户编码',
  `apply_user_id` bigint NOT NULL COMMENT '申请人ID',
  `leave_type` int DEFAULT NULL COMMENT '请假类型',
  `start_time` datetime DEFAULT NULL COMMENT '开始时间',
  `end_time` datetime DEFAULT NULL COMMENT '结束时间',
  `reason` varchar(500) DEFAULT NULL COMMENT '请假事由',
  `status` int DEFAULT '1' COMMENT '审批状态 (1-待审批, 2-已同意, 3-已拒绝)',
  `audit_comment` varchar(500) DEFAULT NULL COMMENT '审批意见',
  `auditor_id` bigint DEFAULT NULL COMMENT '当前审批人ID',
  `auditor_ids` varchar(500) DEFAULT NULL COMMENT '当前候选审批人ID列表',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_leave_code` (`leave_code`),
  KEY `idx_tenant_apply` (`tenant_code`,`apply_user_id`),
  KEY `idx_tenant_status_auditor` (`tenant_code`,`status`,`auditor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='员工请假审批表';
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wechat_subscribe_user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '??ID',
  `tenant_code` varchar(50) NOT NULL,
  `user_id` bigint NOT NULL,
  `openid` varchar(128) NOT NULL,
  `template_id` varchar(128) NOT NULL,
  `subscribe_status` varchar(20) NOT NULL DEFAULT 'accept',
  `create_time` datetime DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_user_template` (`tenant_code`,`user_id`,`template_id`),
  KEY `idx_openid` (`openid`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;
/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;

/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;
