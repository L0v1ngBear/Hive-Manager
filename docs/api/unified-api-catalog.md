# Unified API Catalog

## Contract

All public business routes will use `/api/**`; no `/web/**` compatibility route will remain. Authentication entry points will be separated into `/api/auth/admin/**` and `/api/auth/mini/**` while sharing session, tenant, permission, and user-state services. Domain endpoint matrices will be added as each capability converges.

## Module status

| Module | Status |
| --- | --- |
| foundation | PLANNED |
| permission | PLANNED |
| auth | COMPLETE |

Authentication routes are `POST /api/auth/admin/login`, `POST /api/auth/admin/scan-login/session`, `GET /api/auth/admin/scan-login/status`, `POST /api/auth/admin/scan-login/confirm`, `POST /api/auth/mini/login`, `POST /api/auth/mini/wechat-login`, `GET /api/auth/me`, and `POST /api/auth/logout`. Reset, initial-password, and organization-join routes are retained only below `/api/auth/admin/**`; `/api/auth/login` does not exist.

WeChat login resolves phone matches only inside the bounded tenant set. A phone matching multiple tenants is rejected unless the request supplies an explicit allowed `tenantCode`; the server never selects an unordered tenant candidate.
| order | COMPLETE |
| approval | COMPLETE |
| inventory | PLANNED |
| quality | PLANNED |
| installation | PLANNED |
| customer | PLANNED |
| document | PLANNED |
| equipment | PLANNED |
| print | PLANNED |
| notification | PLANNED |
| attendance | PLANNED |
| migration | PLANNED |
| deployment | PLANNED |
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
| POST | admin `/order/save/{orderId}` | `/api/orders/{orderId}/save` | `order:update` | `SalesOrderSaveRequest` | void | `saveSalesOrder` | RENAMED |
| POST | admin `/order/update/{orderId}`; mini `/orders/{orderId}/status` | `/api/orders/{orderId}/status` | `order:update` | `SalesOrderUpdateRequest` | void | `updateSalesOrder` | MERGED |
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
