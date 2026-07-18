-- Enforce one hashed phone per real tenant without changing existing user data.
-- Tenant-less rows are reported by audit-unified-employee-login.sh for manual reconciliation.

DELIMITER $$

CREATE PROCEDURE guard_unified_employee_login_phone_uniqueness()
BEGIN
    DECLARE duplicate_count BIGINT DEFAULT 0;
    DECLARE index_exists BIGINT DEFAULT 0;

    SELECT COUNT(*)
      INTO duplicate_count
      FROM (
          SELECT tenant_code, phone_hash
          FROM `user`
          WHERE tenant_code IS NOT NULL AND tenant_code <> ''
            AND phone_hash IS NOT NULL AND phone_hash <> ''
          GROUP BY tenant_code, phone_hash
          HAVING COUNT(*) > 1
      ) duplicate_tenant_phones;

    IF duplicate_count > 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Duplicate tenant phone hashes must be resolved before unified employee login migration';
    END IF;

    -- Empty tenant codes represent tenant-less legacy users. Normalize only that sentinel
    -- to NULL so MySQL's unique key leaves those rows outside tenant-phone uniqueness.
    UPDATE `user`
    SET tenant_code = NULL
    WHERE tenant_code = '';

    SELECT COUNT(*)
      INTO index_exists
      FROM information_schema.statistics
      WHERE table_schema = DATABASE()
        AND table_name = 'user'
        AND index_name = 'uk_user_tenant_phone_hash';

    IF index_exists = 0 THEN
        ALTER TABLE `user`
            ADD UNIQUE KEY `uk_user_tenant_phone_hash` (`tenant_code`, `phone_hash`);
    END IF;
END$$

CALL guard_unified_employee_login_phone_uniqueness()$$
DROP PROCEDURE guard_unified_employee_login_phone_uniqueness$$

DELIMITER ;
