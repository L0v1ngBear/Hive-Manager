# Unified API Catalog

## Contract

All public business routes use `/api/**`; no `/web/**` compatibility route remains. Authentication entry points are separated into `/api/auth/admin/**` and `/api/auth/mini/**` while sharing session, tenant, permission, and user-state services. The matrices below are the converged contract consumed by both clients.

## Module status

| Module | Status |
| --- | --- |
| foundation | COMPLETE |
| permission | COMPLETE |
| auth | COMPLETE |

Authentication routes are `POST /api/auth/admin/login`, `POST /api/auth/admin/scan-login/session`, `GET /api/auth/admin/scan-login/status`, `POST /api/auth/admin/scan-login/confirm`, `POST /api/auth/mini/login`, `POST /api/auth/mini/wechat-login`, `GET /api/auth/me`, and `POST /api/auth/logout`. Reset, initial-password, and organization-join routes are retained only below `/api/auth/admin/**`; `/api/auth/login` does not exist.

WeChat login resolves phone matches only inside the bounded tenant set. A phone matching multiple tenants is rejected unless the request supplies an explicit allowed `tenantCode`; the server never selects an unordered tenant candidate.
| order | COMPLETE |
| approval | COMPLETE |
| inventory | COMPLETE |
| quality | COMPLETE |
| installation | COMPLETE |
| customer | COMPLETE |
| document | COMPLETE |
| equipment | COMPLETE |
| label | COMPLETE |
| print | COMPLETE |
| notification | COMPLETE |
| attendance | COMPLETE |
| migration | COMPLETE |
| deployment | PLANNED |

## Task 9 source closure

Every route in this catalog is implemented by a Controller under `my.hive.api` and a single canonical domain service under `my.hive.domain`. The runtime no longer scans or imports `my.management` or `my.hive_back`. Admin authentication writes and mini-program login writes use the shared operation-log aspect with request argument recording disabled so credentials, verification codes, and WeChat login payloads are not persisted in audit arguments.

The generic print-task contract is served only by `my.hive.api.print.PrintTaskController`; the former external-common Controller location is not part of the executable application. All public route roots remain relative to the single `/api` context.

Task 10 does not alter any HTTP contract. Database setup and upgrades are performed only through repository `scripts/migrate-db.sh` and the versioned `db-migrations/migration_manifest.txt`; application classpath SQL is no longer shipped as an alternate migration source.

## Task 11 management UI consumer status

The management UI is migrated to this catalog. Its environment base URL, Axios default, and Vite development proxy are all `/api`; admin login, password reset, organization join, initial-password change, and scan-login polling use `/api/auth/admin/**`. Order, approval, quality, and installation request modules use the canonical routes listed below. No management request fallback to `/web`, singular `/order/**`, `/bad-product/**`, or `/installation-task/**` remains.

`management-ui/tests/unified-api-routes.test.js` is the client-side contract gate. It covers the shared prefix, namespaced admin authentication, canonical resource roots, removed health probe, and retired route patterns.

## Task 12 gateway contract

The deployment nginx configuration exposes one application upstream: every `/api/**` request is forwarded without path rewriting to `backend:8080`. There is no client-specific upstream and no compatibility location. `/health` is an nginx liveness endpoint; application identity and representative admin/mini business probes are completed in Task 13.

## Task 13 health and identity contract

| Method | Route | Authorization | Response | Identity |
| --- | --- | --- | --- | --- |
| GET | `/api/health` | public | `Result<{status,application,version}>` | `X-Hive-Build`, `X-Hive-Instance` |

Every application response carries the same two non-secret headers. `X-Hive-Build` is derived from packaged Spring Boot build properties; `X-Hive-Instance` is generated once when the process starts. Management and mini-program clients therefore share both an artifact identity and a process identity without sharing their authentication entry paths.

The Java smoke contract registers exactly one mapping for `/api/health`, `/api/auth/admin/login`, `/api/auth/mini/login`, `/api/auth/me`, `/api/emp/employee/page`, `/api/orders`, `/api/approval/summary`, `/api/inventory/summary`, `/api/notifications/page`, and `/api/print-task/recent`. The release script accepts expected 200/400/401 outcomes and rejects a missing or divergent identity header.

## Authorization contract

Protected endpoints accept only exact assignable Permission Catalog V3 codes. Wildcards, aliases, prefixes, legacy enum names, and dot-form codes are invalid. Both authentication channels resolve employee state and effective permissions through the same tenant-scoped pipeline.

## Order endpoint convergence matrix

The canonical collection is `/api/orders/**`. `PermissionCatalogV3` constants below are the only accepted permission identifiers; `none` means the operation is authenticated and tenant-scoped but has no finer permission annotation.

| Method | Old route(s) | New route | Permission | Request | Response | Canonical service method | Disposition |
| --- | --- | --- | --- | --- | --- | --- | --- |
| GET | admin `/order/page`; mini `/orders/list` | `/api/orders` | `order:list` | `SalesOrderPageRequest` | `PageResult<SalesOrderPageVO>` | `pageSalesOrders` | MERGED |
| GET | admin `/order/status-summary`; mini `/orders/status-summary` | `/api/orders/status-summary` | `order:list` | none | `Map<String,Long>` | `countSalesOrderStatuses` | MERGED |
| GET | admin `/order/detail/{orderId}`; mini `/orders/detail/{orderId}` | `/api/orders/{orderId}` | `order:detail` | path `orderId` | `SalesOrderDetailVO` | `getSalesOrderDetail` | MERGED |
| GET | mini `/orders/status-log/{orderId}` | `/api/orders/{orderId}/status-log` | `order:detail` | path `orderId` | `List<SalesOrderStatusLogVO>` | `listSalesLogs` | RENAMED |
| POST | admin `/order/create`; mini `/orders/add` | `/api/orders` | `order:create` | `SalesOrderSaveRequest` | `String` order id | `createSalesOrder` | MERGED |
| POST | admin `/order/attachment/upload` | `/api/orders/attachment` | `order:create` | multipart `file` | `SalesOrderAttachmentVO` | `uploadSalesAttachment` | RENAMED |
| GET | admin `/order/attachment/download` | `/api/orders/attachment` | `order:detail` | query `url,name` | resource | `loadSalesAttachment` | RENAMED |
| PUT | admin `/order/save/{orderId}`; mini detail save | `/api/orders/{orderId}` | `order:update` | `SalesOrderSaveRequest` | void | `saveSalesOrder` | MERGED; old save alias removed |
| POST | admin `/order/update/{orderId}`; mini `/orders/{orderId}/status` | `/api/orders/{orderId}/status` | `order:update` | `SalesOrderUpdateRequest` | void | `updateSalesOrder` | MERGED |
| POST | mini production progress and exception updates | `/api/orders/{orderId}/process` | `order:update` plus current status `advance` permission for process changes | `ProductionOrderUpdateRequest` | void | `updateSalesOrderProcess` | MERGED |
| POST | admin `/order/next/{orderId}` | `/api/orders/{orderId}/advance` | `order:update` | `SalesOrderUpdateRequest` | void | `advanceSalesOrderToNextStage` | RENAMED |
| POST | mini `/orders/{flowCode}/flow-advance` | `/api/orders/flow/{flowCode}/advance` | `order:update` | path `flowCode` | void | `advanceSalesOrderByFlowCode` | RENAMED |
| POST | admin `/order/rollback/{orderId}`; mini `/orders/{orderId}/rollback` | `/api/orders/{orderId}/rollback` | `order:update` | `SalesOrderUpdateRequest` | void | `submitSalesOrderRollbackApproval` | MERGED |
| POST | admin `/order/log/{logId}/time` | `/api/orders/status-log/{logId}/time` | `order:update` | `OrderStatusLogTimeCorrectionRequest` | void | `correctSalesLogTime` | RENAMED |
| POST | admin and mini `/order(s)/flow-print-task` | `/api/orders/flow-print-task` | `order:print` | `OrderFlowPrintTaskRequest` | `OrderFlowPrintTaskVO` | `createSalesOrderFlowPrintTask` | MERGED |
| GET | admin `/order/warning/setting` | `/api/orders/warning/setting` | `order:warning:list` | none | `OrderWarningSettingVO` | `getOrderWarningSetting` | RENAMED |
| POST | admin `/order/warning/setting` | `/api/orders/warning/setting` | `order:warning:setting` | `OrderWarningSettingUpdateRequest` | `OrderWarningSettingVO` | `updateOrderWarningSetting` | RENAMED |
| GET | admin `/order/warning/summary` | `/api/orders/warning/summary` | `order:warning:list` | none | `OrderWarningSummaryVO` | `getOrderWarningSummary` | RENAMED |
| POST | admin `/order/warning/refresh`, `/refresh-all`, `/{orderId}/refresh` | `/api/orders/warning/refresh`, `/refresh-all`, `/{orderId}/refresh` | `order:warning:list` | optional path `orderId` | `OrderWarningSummaryVO` | `refreshOrderWarningSummary`, `refreshOrderWarnings`, `refreshOrderWarning` | RENAMED |
| GET | admin `/order/health` | removed | none | none | none | none | REMOVED |

`SalesOrderSaveRequest` and `SalesOrderDetailVO` use `notes[]` instead of the retired single `remark` field. Each note carries `id`, `content`, `version`, updater and update time; saved notes can be modified but cannot be deleted. Visibility and mutation use `order:note:view`, `order:note:create`, and `order:note:update` in addition to the order operation permission.

Order invoice state uses the exact integer contract `0=unissued`, `1=issued`, and `2=other`; null input is normalized to `0`, while every other value is rejected. `SalesOrderPageVO` exposes `invoiceWarning`, `invoiceAgeDays`, and `invoiceWarningDays`. Only unissued orders that are not `pending_cancel` or `cancelled` enter the fixed seven-day warning calculation, measured in complete days from `createTime`; issued and other orders never warn. The status summary exposes `invoice_unpaid`, `invoice_paid`, `invoice_other`, and `invoice_warning`. This invoice warning is independent from the configurable stale-order warning.

Creating or saving a `pending_pay` order is side-effect free for approval. Only `POST /api/orders/{orderId}/advance` from `pending_pay` creates a material-approval candidate, which requires `order:audit:material` when processed.

The unified process endpoint applies one monotonic target across all production tasks linked to the sales order. Lagging tasks advance to the requested process, while tasks already at or beyond that process keep their current value; the endpoint never rolls a production task backward. Shipping, completion, and cancellation remain order-status transitions and are rejected by this endpoint.

## Approval endpoint convergence matrix

All retained routes are rooted at `/api/approval/**` and delegate to the single `ApprovalService` (default-auditor configuration remains a collaborator of that service).

| Method | Old route | New route | Permission | Request | Response | Canonical service method | Disposition |
| --- | --- | --- | --- | --- | --- | --- | --- |
| GET | admin/mini `/approval/summary` | `/api/approval/summary` | none | none | `ApprovalSummaryVO` | `getSummary` | MERGED |
| GET | admin/mini `/approval/auditors` | `/api/approval/auditors` | none | `type,keyword,limit` | `List<ApprovalAuditorOptionVO>` | `listAuditorOptions` | MERGED |
| GET | admin `/approval/default-auditors` | `/api/approval/default-auditors` | `approval:auditor:list` | none | `List<ApprovalDefaultAuditorVO>` | `listDefaultAuditors` | RENAMED |
| POST | admin `/approval/default-auditors` | `/api/approval/default-auditors` | `approval:auditor:setting` | `ApprovalDefaultAuditorSaveRequest` | void | `saveDefaultAuditor` | RENAMED |
| POST | mini `/approval/leave/submit` | `/api/approval/leave` | `approval:leave:submit` | `LeaveSubmitRequest` | `String` | `submitLeave` | RENAMED |
| GET | admin/mini `/approval/leave/list` | `/api/approval/leave` | scope-dependent: `approval:leave:submit`/`approval:leave:list`/`approval:leave:audit` | `scope,status,limit` | `List<LeaveApprovalListVO>` | `listLeaveApprovals` | MERGED |
| GET | admin/mini `/approval/leave/{leaveCode}` | `/api/approval/leave/{leaveCode}` | `approval:leave:detail` | path `leaveCode` | `LeaveDetailVO` | `getLeaveDetail` | MERGED |
| POST | admin/mini `/approval/leave/audit` | `/api/approval/leave/audit` | `approval:leave:audit` | `LeaveAuditRequest` | void | `auditLeave` | MERGED |
| POST | admin/mini `/approval/finance/submit` | `/api/approval/finance` | `approval:finance:submit` | `FinanceSubmitRequest` | `String` | `submitFinance` | RENAMED |
| POST | admin/mini `/approval/finance/attachment/upload` | `/api/approval/finance/attachment` | `approval:finance:submit` | multipart `file` | `BusinessAttachmentVO` | attachment collaborator `upload` | RENAMED |
| GET | admin/mini `/approval/finance/attachment/download` | `/api/approval/finance/attachment` | `approval:finance:detail` | `url,name` | resource | attachment collaborator `load` | RENAMED |
| GET | admin/mini `/approval/finance/list` | `/api/approval/finance` | scope-dependent: `approval:finance:submit`/`approval:finance:list`/`approval:finance:audit` | `scope,status,limit` | `List<FinanceApprovalVO>` | `listFinanceApprovals` | MERGED |
| GET | admin/mini `/approval/finance/{approvalCode}` | `/api/approval/finance/{approvalCode}` | `approval:finance:detail` | path `approvalCode` | `FinanceApprovalVO` | `getFinanceDetail` | MERGED |
| POST | admin/mini `/approval/finance/audit` | `/api/approval/finance/audit` | `approval:finance:audit` | `FinanceAuditRequest` | void | `auditFinance` | MERGED |
| POST | admin/mini `/approval/resignation/submit` | `/api/approval/resignation` | `approval:resignation:submit` | `ResignationSubmitRequest` | `String` | `submitResignation` | RENAMED |
| GET | admin/mini `/approval/resignation/list` | `/api/approval/resignation` | scope-dependent: `approval:resignation:submit`/`approval:resignation:list`/`approval:resignation:audit` | `scope,status,limit` | `List<ResignationApprovalVO>` | `listResignationApprovals` | MERGED |
| GET | admin/mini `/approval/resignation/{resignationCode}` | `/api/approval/resignation/{resignationCode}` | `approval:resignation:detail` | path `resignationCode` | `ResignationApprovalVO` | `getResignationDetail` | MERGED |
| POST | admin/mini `/approval/resignation/audit` | `/api/approval/resignation/audit` | `approval:resignation:audit` | `ResignationAuditRequest` | void | `auditResignation` | MERGED |
| GET | admin/mini `/approval/quality/list` | `/api/approval/quality` | `quality:audit` | `limit` | `List<QualityApprovalVO>` | `listQualityApprovals` | MERGED |
| GET | admin/mini `/approval/quality/{defectiveId}` | `/api/approval/quality/{defectiveId}` | `quality:audit` | path `defectiveId` | `QualityApprovalVO` | `getQualityApprovalDetail` | MERGED |
| POST | admin/mini `/approval/quality/audit` | `/api/approval/quality/audit` | `quality:audit` | `QualityAuditRequest` | void | `auditQuality` | MERGED |
| GET | admin/mini `/approval/order/list` | `/api/approval/order` | `approval:list` | `limit` | `List<OrderApprovalVO>` | `listOrderApprovals` | MERGED |
| GET | admin/mini `/approval/order/{orderType}/{orderId}` | `/api/approval/order/{orderType}/{orderId}` | `approval:list` | path `orderType,orderId` | `OrderApprovalVO` | `getOrderApprovalDetail` | MERGED |
| POST | admin/mini `/approval/order/audit` | `/api/approval/order/audit` | `approval:list` | `OrderApprovalAuditRequest` | void | `auditOrder` | MERGED |

## Inventory, quality, and installation convergence matrix

The retained routes are rooted at `/api/inventory/**`, `/api/quality/**`, and `/api/installation-tasks/**`. Old management package implementations under `my.management.module.inventory`, `my.management.module.badproduct`, and `my.management.module.installation` were moved into the canonical `my.hive.domain.*` services; dependent dashboard, notification, approval, and order flows now import those canonical services.

| Method | Old route | New route | Permission | Request | Response | Canonical service method | Disposition |
| --- | --- | --- | --- | --- | --- | --- | --- |
| GET | admin `/inventory/summary` | `/api/inventory/summary` | `inventory:list` | none | `InventorySummaryVO` | `summary` | RENAMED |
| GET | admin `/inventory/page` | `/api/inventory/page` | `inventory:list` | `InventoryPageRequest` | `PageResult<ClothInventoryVO>` | `page` | RENAMED |
| GET | admin `/inventory/model/page` | `/api/inventory/model/page` | `inventory:list` | `InventoryPageRequest` | `PageResult<InventoryModelSummaryVO>` | `modelPage` | RENAMED |
| GET | admin `/inventory/model/detail` | `/api/inventory/model/detail` | `inventory:detail` | `modelCode,spec,status,timeOrder` | `List<ClothInventoryVO>` | `modelDetail` | RENAMED |
| GET | admin `/inventory/cloth/detail` | `/api/inventory/cloth/detail` | `inventory:detail` | `id,barcode` | `ClothInventoryDetailVO` | `clothDetail` | RENAMED |
| POST | admin `/inventory/cloth/in` | `/api/inventory/cloth/in` | `inventory:cloth:in` | `InventoryInRequest` | `InventoryInResultVO` | `in` | RENAMED |
| POST | admin `/inventory/cloth/out` | `/api/inventory/cloth/out` | `inventory:cloth:out` | `InventoryOutRequest` | void | `out` | RENAMED |
| POST | admin `/inventory/cloth/image-recognition` | `/api/inventory/cloth/image-recognition` | `inventory:cloth:in` | multipart `file` | `InventoryImageRecognitionVO` | `recognizeInboundImage` | RENAMED |
| GET | admin `/inventory/warning/list` | `/api/inventory/warning/list` | `inventory:warning:list` | none | `List<InventoryWarningVO>` | `warnings` | RENAMED |
| GET/POST | admin `/inventory/warning/setting` | `/api/inventory/warning/setting` | `inventory:warning:list` / `inventory:warning:setting` | `InventoryWarningSettingUpdateRequest` | `InventoryWarningSettingVO` | `currentSetting`, `updateCurrentSetting` | RENAMED |
| GET | admin `/inventory/record/recent` | `/api/inventory/record/recent` | `inventory:record:list` | none | `List<InventoryRecordVO>` | `recentRecords` | RENAMED |
| GET | admin `/inventory/trend` | `/api/inventory/trend` | `inventory:trend` | none | `List<InventoryTrendVO>` | `trend` | RENAMED |
| GET | admin `/inventory/model/search` | `/api/inventory/model/search` | `inventory:model:search` | `keyword` | `List<InventoryModelOptionVO>` | `searchModels` | RENAMED |
| GET | admin `/inventory/barCode/search` | `/api/inventory/barCode/search` | `inventory:barcode:search` | `barCode` | `ClothInventoryVO` | `searchByBarcode` | RENAMED |
| GET/POST | admin `/inventory/import-template`, `/inventory/import` | `/api/inventory/import-template`, `/api/inventory/import` | `inventory:import` | multipart `file` | `InventoryImportResultVO` | `downloadImportTemplate`, `importInventory` | RENAMED |
| GET | admin `/bad-product/list` | `/api/quality/list` | `quality:list` | `BadProductPageRequest` | `PageResult<BadProductVO>` | `page` | RENAMED |
| POST | admin `/bad-product/save` | `/api/quality/save` | `quality:create` | `BadProductSaveRequest` | void | `save` | RENAMED |
| POST | admin `/bad-product/process` | `/api/quality/process` | `quality:process` | `BadProductProcessRequest` | void | `process` | RENAMED |
| POST/GET | admin `/bad-product/attachment/*` | `/api/quality/attachment/*` | `quality:attachment:upload/download` | multipart `file`, `url,name` | `BusinessAttachmentVO` / resource | `uploadAttachment`, `loadAttachment` | RENAMED |
| GET | admin `/installation-task/page` | `/api/installation-tasks/page` | `installation:list` | `InstallationTaskPageRequest` | `PageResult<InstallationTaskVO>` | `page` | RENAMED |
| POST | admin `/installation-task/status` | `/api/installation-tasks/status` | `installation:update` | `InstallationTaskStatusUpdateRequest` | `InstallationTaskVO` | `updateStatus` | RENAMED |
| POST/GET | admin `/installation-task/attachment/*` | `/api/installation-tasks/attachment/*` | `installation:attachment:upload/download` | multipart `file`, `url,name` | `BusinessAttachmentVO` / resource | `uploadAttachment`, `loadAttachment` | RENAMED |

Installation status updates accept `installers: [{name, phone}]`. A row requires both trimmed values; names are limited to 50 characters, phones to 40 characters, at most 20 rows are accepted, and an identical trimmed name/phone pair cannot occur twice. Phone values are free text so mobile, landline, and extension formats are all supported. `completed_accepted` requires at least one installer; other states allow an empty or omitted list.

`InstallationTaskVO.installers` returns ordered rows shaped `{id, name, phone, sortOrder}`. The service loads all installer rows for a non-empty task page in one tenant-scoped `IN` query and performs status update plus full detail replacement in one transaction. Every installer query, delete, and insert includes `tenant_code`. The retired single-person request/response fields are not accepted or returned, and no compatibility route was added.

## Customer, document, equipment, label, and print convergence matrix

The retained routes are rooted at `/api/customer/**`, `/api/document/**`, `/api/equipment/**`, `/api/label-template/**`, `/api/receipt/**`, and `/api/print-task/**`. The old management package implementations under `my.management.module.customer`, `my.management.module.document`, `my.management.module.equipment`, `my.management.module.label`, and `my.management.module.receipt` were moved into canonical `my.hive.domain.*` packages. Generic print-task endpoints are served by exactly one `PrintTaskController` under `my.hive.api.print`.

| Domain | Old route root | New route root | Canonical service |
| --- | --- | --- | --- |
| customer | admin `/customer` | `/api/customer` | `my.hive.domain.customer.service.CustomerService` |
| document | admin `/document` | `/api/document` | `my.hive.domain.document.service.DocumentService` |
| equipment | admin `/equipment` | `/api/equipment` | `my.hive.domain.equipment.service.EquipmentService` |
| label | admin `/label-template` | `/api/label-template` | `my.hive.domain.label.service.LabelTemplateService` |
| receipt print | admin `/receipt` | `/api/receipt` | `my.hive.domain.print.receipt.service.ReceiptPrintService` |
| print task | common `/print-task` | `/api/print-task` | `my.hive.domain.print.PrintTaskService` |

## Notification, WeChat subscription, and attendance convergence matrix

All rows below are served by the same `/api` process. Notification read/close operations and personal attendance records always derive tenant and user identity from the shared authenticated context.

| Method | Route | Permission | Canonical service method |
| --- | --- | --- | --- |
| GET | `/api/notifications/page` | authenticated | `NotificationService.page` |
| GET | `/api/notifications/unread` | authenticated | `NotificationService.unread` |
| GET | `/api/notifications/unread-count` | authenticated | `NotificationService.unreadCount` |
| GET | `/api/notifications/announcements` | `notification:announcement:list` | `EnterpriseAnnouncementService.announcements` |
| POST | `/api/notifications/announcements` | `notification:announcement:publish` | `EnterpriseAnnouncementService.publishAnnouncement` |
| POST | `/api/notifications/{id}/read` | authenticated, current user only | `NotificationService.markRead` |
| POST | `/api/notifications/{id}/close` | authenticated, current user only | `NotificationService.closeTask` |
| POST | `/api/notifications/sync` | `notification:announcement:publish` | `NotificationService.syncAllNotificationsForCurrentTenant` |
| GET | `/api/wechat/subscriptions/config` | authenticated | `WechatSubscribeService.config` |
| POST | `/api/wechat/subscriptions/register` | authenticated | `WechatSubscribeService.register` |
| GET | `/api/attendance/summary` | `attendance:record:list` | `AttendanceService.summary` |
| GET | `/api/attendance/page` | `attendance:record:list` | `AttendanceService.page` |
| GET | `/api/attendance/departments` | `attendance:record:list` | `AttendanceService.departments` |
| GET | `/api/attendance/rule` | `attendance:rule:list` | `AttendanceService.getRule` |
| POST | `/api/attendance/rule/save` | `attendance:rule:update` | `AttendanceService.saveRule` |
| GET | `/api/attendance/export-excel` | `attendance:export` | `AttendanceService.exportExcel` |
| POST | `/api/attendance/punch` | `attendance:punch` | `AttendanceService.punch` |
| GET | `/api/attendance/records/me` | `attendance:record:list` | `AttendanceService.recordsForCurrentUser` |

## Management UI integration verification

The Element Plus management UI merged at `c1d3733` continues to call only this catalog's `/api` routes. Its 177 source-contract tests verify the unified prefix, canonical auth/order/approval/quality/installation families, exact V3 permission leaves, information-channel order contract, and single deployment topology. No `/web` client fallback or legacy permission alias was added by the UI merge.
