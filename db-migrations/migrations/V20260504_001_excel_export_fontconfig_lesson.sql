-- Seed the Excel export fontconfig incident into AI self-evolution samples.
-- Root lesson: Excel export must not depend on server fontconfig; simple exports should use EasyExcel.

INSERT INTO ai_advice_training_sample (
    tenant_code,
    sample_key,
    category,
    title,
    source_type,
    priority,
    confidence,
    input_snapshot_json,
    behavior_context_json,
    advice_json,
    label_status,
    feedback_type,
    feedback_text,
    feedback_time,
    occurrence_count,
    create_time,
    update_time
) VALUES (
    'PLATFORM',
    'PLATFORM_LESSON:20260504:excel-export-fontconfig-easyexcel',
    'platform_export',
    'Excel export should not depend on container fonts',
    'platform_lessons',
    'P1',
    97,
    JSON_OBJECT(
        'incidentType', 'excel_export_fontconfig',
        'symptom', 'Fontconfig head is null, check your fonts or fonts configuration',
        'stack', JSON_ARRAY('Apache POI SheetUtil', 'XSSFSheet.autoSizeColumn', 'ExcelUtil.autoSize', 'AttendanceManageService.exportExcel'),
        'rootCause', 'Minimal Alpine/Corretto containers can miss fontconfig or usable fonts; POI autoSizeColumn initializes AWT fonts and may fail.',
        'finalFix', 'Switch simple exports and templates to EasyExcel response writer; keep POI only for import reading.'
    ),
    JSON_OBJECT('source', 'codex_project_memory', 'capturedAt', '2026-05-05'),
    JSON_OBJECT(
        'title', 'Use EasyExcel for simple business exports',
        'summary', 'Column auto-size is a UI enhancement and must never break attendance, employee, or price export.',
        'suggestion', 'Write export responses through ExcelUtil.writeRowsToResponse / writeTemplateToResponse backed by EasyExcel. Keep POI WorkbookFactory for imports until import parsing is separately migrated.',
        'firstAction', 'When adding a new export endpoint, use EasyExcel writer methods instead of creating XSSFWorkbook.',
        'reviewMetric', 'No-font Linux container can export attendance, employee, and price files successfully.'
    ),
    'resolved',
    'resolved',
    'Changed export writer from POI Workbook/autoSizeColumn to EasyExcel. POI remains only for import reading.',
    NOW(),
    1,
    NOW(),
    NOW()
)
ON DUPLICATE KEY UPDATE
    category = VALUES(category),
    title = VALUES(title),
    source_type = VALUES(source_type),
    priority = VALUES(priority),
    confidence = VALUES(confidence),
    input_snapshot_json = VALUES(input_snapshot_json),
    behavior_context_json = VALUES(behavior_context_json),
    advice_json = VALUES(advice_json),
    label_status = VALUES(label_status),
    feedback_type = VALUES(feedback_type),
    feedback_text = VALUES(feedback_text),
    feedback_time = VALUES(feedback_time),
    occurrence_count = GREATEST(occurrence_count, VALUES(occurrence_count)),
    update_time = NOW();
