ALTER TABLE label_template
    ADD COLUMN design_json JSON NULL COMMENT '可视化设计器结构' AFTER content,
    ADD COLUMN width_mm DECIMAL(10,2) NOT NULL DEFAULT 70.00 COMMENT '标签宽度mm' AFTER design_json,
    ADD COLUMN height_mm DECIMAL(10,2) NOT NULL DEFAULT 50.00 COMMENT '标签高度mm' AFTER width_mm;

