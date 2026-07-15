-- Production-order print tasks belong to the retired split-order workflow.
-- Cancel only tasks that could still appear in a printer queue.

UPDATE print_task
SET status = 3,
    error_message = 'Cancelled after unified order migration',
    update_time = CURRENT_TIMESTAMP
WHERE print_type = 'order_flow'
  AND biz_type = 'production_order'
  AND status IN (0, 2);
