-- V20260514_002_resignation_approval.sql
-- Purpose:
-- 1. Add resignation approval as a first-class approval flow.
-- 2. Seed resignation permissions and keep the built-in EMPLOYEE role safe.
-- 3. Existing data is not overwritten or deleted.

CREATE TABLE IF NOT EXISTS employee_resignation_approval (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    resignation_code VARCHAR(50) NOT NULL COMMENT '离职审批单号',
    tenant_code VARCHAR(50) NOT NULL COMMENT '租户编码',
    apply_user_id BIGINT NOT NULL COMMENT '申请人ID',
    expected_leave_date DATE NOT NULL COMMENT '预计离职日期',
    reason VARCHAR(500) NOT NULL COMMENT '离职原因',
    handover_note VARCHAR(500) DEFAULT NULL COMMENT '交接说明',
    status INT NOT NULL DEFAULT 1 COMMENT '审批状态：1待审批 2已通过 3已拒绝',
    auditor_id BIGINT DEFAULT NULL COMMENT '当前审批人ID',
    audit_comment VARCHAR(500) DEFAULT NULL COMMENT '审批意见',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_resignation_code (resignation_code),
    KEY idx_resign_tenant_apply_status_time (tenant_code, apply_user_id, status, create_time),
    KEY idx_resign_tenant_auditor_status_time (tenant_code, auditor_id, status, create_time),
    KEY idx_resign_tenant_status_time (tenant_code, status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工离职审批表';

INSERT INTO sys_permission (id, parent_id, perm_code, perm_type, sort, perm_name, create_time, update_time, is_deleted)
SELECT 730, parent.id, 'approval:resignation', 2, 3, '离职审批', NOW(), NOW(), 0
FROM sys_permission parent
WHERE BINARY parent.perm_code = BINARY 'approval'
  AND IFNULL(parent.is_deleted, 0) = 0
  AND NOT EXISTS (
      SELECT 1 FROM sys_permission existed
      WHERE BINARY existed.perm_code = BINARY 'approval:resignation'
        AND IFNULL(existed.is_deleted, 0) = 0
  )
LIMIT 1;

INSERT INTO sys_permission (id, parent_id, perm_code, perm_type, sort, perm_name, create_time, update_time, is_deleted)
SELECT seed.id, parent.id, seed.perm_code, 3, seed.sort_no, seed.perm_name, NOW(), NOW(), 0
FROM (
    SELECT 731 AS id, 'approval:resignation:submit' AS perm_code, 1 AS sort_no, '提交离职申请' AS perm_name
    UNION ALL SELECT 732, 'approval:resignation:audit', 2, '审批离职单'
    UNION ALL SELECT 733, 'approval:resignation:detail', 3, '离职单详情'
) seed
INNER JOIN sys_permission parent
    ON BINARY parent.perm_code = BINARY 'approval:resignation'
   AND IFNULL(parent.is_deleted, 0) = 0
WHERE NOT EXISTS (
    SELECT 1 FROM sys_permission existed
    WHERE BINARY existed.perm_code = BINARY seed.perm_code
      AND IFNULL(existed.is_deleted, 0) = 0
);

CREATE TEMPORARY TABLE IF NOT EXISTS tmp_resignation_employee_perm (
    perm_code VARCHAR(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL,
    PRIMARY KEY (perm_code)
) ENGINE=MEMORY;

TRUNCATE TABLE tmp_resignation_employee_perm;

INSERT INTO tmp_resignation_employee_perm (perm_code) VALUES
('approval:resignation:submit'),
('approval:resignation:detail');

INSERT INTO sys_role_permission (role_id, permission_id, create_time, is_deleted)
SELECT r.id, p.id, NOW(), 0
FROM sys_role r
INNER JOIN sys_permission p
INNER JOIN tmp_resignation_employee_perm allow_perm
    ON BINARY allow_perm.perm_code = BINARY p.perm_code
WHERE BINARY r.role_code = BINARY 'EMPLOYEE'
  AND IFNULL(r.is_deleted, 0) = 0
  AND IFNULL(p.is_deleted, 0) = 0
ON DUPLICATE KEY UPDATE is_deleted = 0;

UPDATE sys_role_permission rp
INNER JOIN sys_role r ON r.id = rp.role_id
INNER JOIN sys_permission p ON p.id = rp.permission_id
SET rp.is_deleted = 1
WHERE BINARY r.role_code = BINARY 'EMPLOYEE'
  AND IFNULL(r.is_deleted, 0) = 0
  AND IFNULL(rp.is_deleted, 0) = 0
  AND BINARY p.perm_code = BINARY 'approval:resignation:audit';

DROP TEMPORARY TABLE IF EXISTS tmp_resignation_employee_perm;
