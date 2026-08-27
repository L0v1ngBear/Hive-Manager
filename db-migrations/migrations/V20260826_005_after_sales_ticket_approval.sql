-- 售后工单可选审批：审批候选人沿用 approval_auditor_candidate，字段仅保存当前审批快照。
ALTER TABLE after_sales_ticket
  ADD COLUMN approval_required TINYINT NOT NULL DEFAULT 0 COMMENT '是否需要审核' AFTER attachment_urls_json,
  ADD COLUMN approval_status VARCHAR(24) NULL COMMENT 'pending/approved/rejected' AFTER approval_required,
  ADD COLUMN approval_auditor_id BIGINT NULL AFTER approval_status,
  ADD COLUMN approval_auditor_ids VARCHAR(500) NULL AFTER approval_auditor_id;
