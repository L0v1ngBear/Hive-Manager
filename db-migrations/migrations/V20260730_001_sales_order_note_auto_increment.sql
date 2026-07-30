-- Generate future sales-order note IDs in MySQL while preserving every existing note ID.
-- Existing Snowflake-sized IDs remain valid and are exposed to browsers as strings.

ALTER TABLE `sales_order_note`
  MODIFY COLUMN `id` bigint NOT NULL AUTO_INCREMENT;
