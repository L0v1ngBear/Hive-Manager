-- Organization departments and positions now use delete-only lifecycle management.
-- Preserve all non-deleted records and reactivate historic disabled rows so they
-- remain assignable until an administrator explicitly deletes them.

UPDATE `emp_department`
SET `status` = 1,
    `update_time` = CURRENT_TIMESTAMP
WHERE `is_deleted` = 0
  AND COALESCE(`status`, 0) <> 1;

UPDATE `emp_position`
SET `status` = 1,
    `update_time` = CURRENT_TIMESTAMP
WHERE `is_deleted` = 0
  AND COALESCE(`status`, 0) <> 1;
