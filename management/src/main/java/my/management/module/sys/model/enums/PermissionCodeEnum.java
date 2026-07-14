package my.management.module.sys.model.enums;

/**
 * V3 tenant permission leaves used by management runtime authorization.
 * All constants except suffix helpers must be assignable entries in PermissionCatalogV3.
 */
public final class PermissionCodeEnum {

    public static final String CODE_APPROVAL_LIST = "approval:list";
    public static final String CODE_APPROVAL_AUDITOR_LIST = "approval:auditor:list";
    public static final String CODE_APPROVAL_AUDITOR_SETTING = "approval:auditor:setting";
    public static final String CODE_APPROVAL_FINANCE_LIST = "approval:finance:list";
    public static final String CODE_APPROVAL_FINANCE_AUDIT = "approval:finance:audit";
    public static final String CODE_APPROVAL_FINANCE_DETAIL = "approval:finance:detail";
    public static final String CODE_APPROVAL_FINANCE_SUBMIT = "approval:finance:submit";
    public static final String CODE_APPROVAL_LEAVE_LIST = "approval:leave:list";
    public static final String CODE_APPROVAL_LEAVE_AUDIT = "approval:leave:audit";
    public static final String CODE_APPROVAL_LEAVE_DETAIL = "approval:leave:detail";
    public static final String CODE_APPROVAL_LEAVE_SUBMIT = "approval:leave:submit";
    public static final String CODE_APPROVAL_RESIGNATION_LIST = "approval:resignation:list";
    public static final String CODE_APPROVAL_RESIGNATION_AUDIT = "approval:resignation:audit";
    public static final String CODE_APPROVAL_RESIGNATION_DETAIL = "approval:resignation:detail";
    public static final String CODE_APPROVAL_RESIGNATION_SUBMIT = "approval:resignation:submit";
    public static final String CODE_ORDER_AUDIT_SHIPMENT = "order:audit:shipment";
    public static final String CODE_ORDER_AUDIT_CANCEL = "order:audit:cancel";

    public static final String CODE_ATTENDANCE_PUNCH = "attendance:punch";
    public static final String CODE_ATTENDANCE_RECORD_LIST = "attendance:record:list";
    public static final String CODE_ATTENDANCE_RULE_LIST = "attendance:rule:list";
    public static final String CODE_ATTENDANCE_RULE_UPDATE = "attendance:rule:update";
    public static final String CODE_ATTENDANCE_EXPORT = "attendance:export";

    public static final String CODE_QUALITY_LIST = "quality:list";
    public static final String CODE_QUALITY_DETAIL = "quality:detail";
    public static final String CODE_QUALITY_CREATE = "quality:create";
    public static final String CODE_QUALITY_UPDATE = "quality:update";
    public static final String CODE_QUALITY_PROCESS = "quality:process";
    public static final String CODE_QUALITY_AUDIT = "quality:audit";
    public static final String CODE_QUALITY_ATTACHMENT_UPLOAD = "quality:attachment:upload";
    public static final String CODE_QUALITY_ATTACHMENT_DOWNLOAD = "quality:attachment:download";
    public static final String CODE_QUALITY_EXPORT = "quality:export";

    public static final String CODE_CUSTOMER_LIST = "customer:list";
    public static final String CODE_CUSTOMER_DETAIL = "customer:detail";
    public static final String CODE_CUSTOMER_CREATE = "customer:create";
    public static final String CODE_CUSTOMER_UPDATE = "customer:update";
    public static final String CODE_CUSTOMER_DELETE = "customer:delete";
    public static final String CODE_CUSTOMER_IMPORT = "customer:import";
    public static final String CODE_CUSTOMER_EXPORT = "customer:export";

    public static final String CODE_DOCUMENT_LIST = "document:list";
    public static final String CODE_DOCUMENT_FOLDER_CREATE = "document:folder:create";
    public static final String CODE_DOCUMENT_FILE_UPLOAD = "document:file:upload";
    public static final String CODE_DOCUMENT_FILE_DOWNLOAD = "document:file:download";
    public static final String CODE_DOCUMENT_RENAME = "document:rename";
    public static final String CODE_DOCUMENT_MOVE = "document:move";
    public static final String CODE_DOCUMENT_DELETE = "document:delete";
    public static final String CODE_DOCUMENT_EXPORT = "document:export";

    public static final String CODE_EMPLOYEE_LIST = "employee:list";
    public static final String CODE_EMPLOYEE_DETAIL = "employee:detail";
    public static final String CODE_EMPLOYEE_CREATE = "employee:create";
    public static final String CODE_EMPLOYEE_UPDATE = "employee:update";
    public static final String CODE_EMPLOYEE_STATUS = "employee:status";
    public static final String CODE_EMPLOYEE_DELETE = "employee:delete";
    public static final String CODE_EMPLOYEE_IMPORT = "employee:import";
    public static final String CODE_EMPLOYEE_EXPORT = "employee:export";
    public static final String CODE_EMPLOYEE_PERMISSION_MANAGE = "employee:permission:manage";

    public static final String CODE_EQUIPMENT_LIST = "equipment:list";
    public static final String CODE_EQUIPMENT_DETAIL = "equipment:detail";
    public static final String CODE_EQUIPMENT_CREATE = "equipment:create";
    public static final String CODE_EQUIPMENT_UPDATE = "equipment:update";
    public static final String CODE_EQUIPMENT_DISABLE = "equipment:disable";
    public static final String CODE_EQUIPMENT_INSPECTION_LIST = "equipment:inspection:list";
    public static final String CODE_EQUIPMENT_INSPECTION_SUBMIT = "equipment:inspection:submit";
    public static final String CODE_EQUIPMENT_EXPORT = "equipment:export";

    public static final String CODE_INVENTORY_LIST = "inventory:list";
    public static final String CODE_INVENTORY_DETAIL = "inventory:detail";
    public static final String CODE_INVENTORY_WARNING_LIST = "inventory:warning:list";
    public static final String CODE_INVENTORY_WARNING_SETTING = "inventory:warning:setting";
    public static final String CODE_INVENTORY_RECORD_LIST = "inventory:record:list";
    public static final String CODE_INVENTORY_TREND = "inventory:trend";
    public static final String CODE_INVENTORY_BARCODE_SEARCH = "inventory:barcode:search";
    public static final String CODE_INVENTORY_MODEL_SEARCH = "inventory:model:search";
    public static final String CODE_INVENTORY_CLOTH_IN = "inventory:cloth:in";
    public static final String CODE_INVENTORY_CLOTH_OUT = "inventory:cloth:out";
    public static final String CODE_INVENTORY_IMPORT = "inventory:import";
    public static final String CODE_INVENTORY_EXPORT = "inventory:export";

    public static final String CODE_INSTALLATION_LIST = "installation:list";
    public static final String CODE_INSTALLATION_DETAIL = "installation:detail";
    public static final String CODE_INSTALLATION_UPDATE = "installation:update";
    public static final String CODE_INSTALLATION_ATTACHMENT_UPLOAD = "installation:attachment:upload";
    public static final String CODE_INSTALLATION_ATTACHMENT_DOWNLOAD = "installation:attachment:download";
    public static final String CODE_INSTALLATION_EXPORT = "installation:export";

    public static final String CODE_PRINT_LABEL_LIST = "print:label:list";
    public static final String CODE_PRINT_LABEL_DETAIL = "print:label:detail";
    public static final String CODE_PRINT_LABEL_CREATE = "print:label:create";
    public static final String CODE_PRINT_LABEL_UPDATE = "print:label:update";
    public static final String CODE_PRINT_LABEL_UPLOAD = "print:label:upload";
    public static final String CODE_PRINT_LABEL_DEFAULT = "print:label:default";
    public static final String CODE_PRINT_LABEL_DISABLE = "print:label:disable";
    public static final String CODE_PRINT_RECEIPT_LIST = "print:receipt:list";
    public static final String CODE_PRINT_RECEIPT_DETAIL = "print:receipt:detail";
    public static final String CODE_PRINT_RECEIPT_EXECUTE = "print:receipt:execute";
    public static final String CODE_PRINT_RECEIPT_UPDATE = "print:receipt:update";
    public static final String CODE_PRINT_RECEIPT_CANCEL = "print:receipt:cancel";

    public static final String CODE_NOTIFICATION_ANNOUNCEMENT_LIST = "notification:announcement:list";
    public static final String CODE_NOTIFICATION_ANNOUNCEMENT_PUBLISH = "notification:announcement:publish";

    public static final String CODE_ORDER_LIST = "order:list";
    public static final String CODE_ORDER_DETAIL = "order:detail";
    public static final String CODE_ORDER_CREATE = "order:create";
    public static final String CODE_ORDER_UPDATE = "order:update";
    public static final String CODE_ORDER_PRINT = "order:print";
    public static final String CODE_ORDER_WARNING_LIST = "order:warning:list";
    public static final String CODE_ORDER_WARNING_SETTING = "order:warning:setting";
    public static final String CODE_ORDER_STATUS_PREFIX = "order:status:";

    public static final String CODE_PRICE_LIST = "price:list";
    public static final String CODE_PRICE_DETAIL = "price:detail";
    public static final String CODE_PRICE_CREATE = "price:create";
    public static final String CODE_PRICE_UPDATE = "price:update";
    public static final String CODE_PRICE_PUBLISH = "price:publish";
    public static final String CODE_PRICE_DELETE = "price:delete";
    public static final String CODE_PRICE_IMPORT = "price:import";
    public static final String CODE_PRICE_EXPORT = "price:export";

    public static final String CODE_ROLE_LIST = "role:list";
    public static final String CODE_ROLE_CREATE = "role:create";
    public static final String CODE_ROLE_UPDATE = "role:update";
    public static final String CODE_ROLE_DELETE = "role:delete";
    public static final String CODE_ROLE_PERMISSION_LIST = "role:permission:list";
    public static final String CODE_ROLE_PERMISSION_UPDATE = "role:permission:update";

    private PermissionCodeEnum() {
    }
}
