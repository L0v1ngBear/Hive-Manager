ALTER TABLE customer_project
    ADD COLUMN construction_area VARCHAR(255) NULL COMMENT '施工区域' AFTER project_name;
