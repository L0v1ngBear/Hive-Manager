-- 售后回访支持独立多图记录；NULL 兼容既有无图片的历史回访。
ALTER TABLE `after_sales_ticket`
  ADD COLUMN `follow_up_images_json` JSON NULL COMMENT '回访图片列表，最多9张' AFTER `follow_up_content`;
