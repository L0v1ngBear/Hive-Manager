-- ORDER-only approval rule update.
-- Other approval types (after-sales, quality, finance, leave and resignation) are intentionally unchanged.

UPDATE approval_default_auditor
SET approval_mode = 'OR',
    update_time = CURRENT_TIMESTAMP
WHERE approval_type = 'ORDER'
  AND (approval_mode IS NULL OR approval_mode <> 'OR');

-- Update both active and closed ORDER candidate snapshots so historical displays use the same rule.
UPDATE approval_auditor_candidate
SET approval_mode = 'OR'
WHERE approval_type = 'ORDER'
  AND (approval_mode IS NULL OR approval_mode <> 'OR');

-- Active ORDER groups containing at least one approved and one pending candidate are completed by
-- OrderApprovalOrReconciliationRunner on backend startup through the normal order business service.
-- This preserves rollback, cancellation, logistics validation, status logs and linked-order sync.
