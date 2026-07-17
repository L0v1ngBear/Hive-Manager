# Task 5 管理端多物流实现报告

## 范围

- 分支：`codex/order-multi-shipment`
- 起始 HEAD：`0355f99692a1bca64cb1edf3d6986dd0dd5c9357`
- 业务代码仅修改 `management-ui`，未修改小程序前端或后端。

## TDD RED 证据

先新增/更新合同测试，再运行：

```powershell
cd D:\HiveManager\management-ui
node --test tests/order-multi-shipment-ui.test.js tests/order-logistics-tracking.test.js tests/order-information-channel-and-advance.test.js tests/order-permission-hardening.test.js
```

结果：退出码 `1`，共 `23` 个测试，`15` 通过、`8` 失败。失败均为预期的旧实现差异：

- 列表和详情仍读取 `expressCompany` / `expressNo` 标量。
- 编辑表单没有 `orderForm.shipments`、新增/放弃和多行校验。
- 物流追踪仍按订单旧端点查询，状态未按 shipment 隔离。
- shipped 推进仍发送物流公司/单号标量。
- 导出仍输出单个 `expressNo`。

## GREEN 证据

实现后重新运行同一命令：

```text
tests 23
pass 23
fail 0
duration_ms 481.0633
exit code 0
```

扩展订单合同回归：

```powershell
node --test tests/order-*.test.js
```

```text
tests 35
pass 35
fail 0
duration_ms 1069.6722
exit code 0
```

生产构建：

```powershell
npm run build
```

```text
vite v8.1.3
1861 modules transformed
built in 9.71s
exit code 0
```

## 改动文件

- `management-ui/src/views/function/order/order.vue`
- `management-ui/src/views/function/order/api/order.js`
- `management-ui/tests/order-multi-shipment-ui.test.js`
- `management-ui/tests/order-logistics-tracking.test.js`
- `management-ui/tests/order-information-channel-and-advance.test.js`
- `.superpowers/sdd/task-5-report.md`

## 实现摘要

- 订单表单使用 `orderForm.shipments`，保留 `id`、`version` 和审计展示字段，提交仅发送 `id`、`logisticsCompany`、`trackingNo`、`version`。
- 最多 50 条；每条公司和单号必填；物流单号不可重复；只有未保存行可放弃，已保存行可修改且无删除入口。
- shipped 状态或推进到 shipped 时要求至少一条完整物流记录，推进 payload 发送 `shipments`。
- 列表按接口数组顺序展示全部物流单号，多条显示“共 N 单”；每条单号拥有独立 popover。
- popover 仅在 `@show` 时调用 `getOrderLogisticsTracking(orderId, shipmentId)`，状态键为 `orderId::shipmentId`，列表加载不预查。
- API 仅保留 `/orders/{orderId}/shipments/{shipmentId}/logistics-tracking`。
- 导出使用中文顿号“、”连接全部物流单号，详情区展示全部物流及最后修改人/时间。

## 自审

- `rg` 检查确认订单页面没有 `orderForm.expressCompany`、`orderForm.expressNo`、`row.expressCompany`、`row.expressNo`、`orderDetail.expressCompany` 或 `orderDetail.expressNo` 残留。
- `rg` 检查确认管理端订单 API 没有旧 `/orders/{orderId}/logistics-tracking` 调用。
- `loadOrders` 合同确认不调用物流追踪接口。
- 已保存 shipment 行没有删除/放弃按钮；放弃按钮受 `v-if="!shipment.id"` 限制。
- 权限守卫、订单状态流转顺序、备注、信息渠道、附件和其他订单业务逻辑未改动。
- `git diff --check` 无空白错误；仅有仓库现有 Windows 行尾提示。
- 中文源文件按 UTF-8 读取、编辑和构建，合同测试直接匹配中文文案，未发现乱码。

## Review Remediation (2026-07-17, base `ab0d227`)

### RED

Frontend contract command:

```powershell
cd D:\HiveManager\management-ui
node --test tests/order-logistics-tracking.test.js tests/order-multi-shipment-ui.test.js tests/order-information-channel-and-advance.test.js tests/order-flow-behavior.test.js tests/order-permission-hardening.test.js
```

Result: exit code `1`; 26 tests, 24 passed and 2 failed. The failures proved that the tracking state key omitted current company/tracking/version and that the table still used the `expressNo` internal column key.

Backend contract command:

```powershell
cd D:\HiveManager\management
.\mvnw.cmd "-Dtest=OrderLogisticsTrackingServiceTest,OrderMultiShipmentLifecycleTest" test
```

Result: `BUILD FAILURE`; 16 tests, 2 failures and 3 errors. The DTO reflection contract could not find `SalesOrderUpdateRequest.shipments`; logistics mocks accepted only the corrected company-aware fingerprint, exposing that production still generated the old identity.

### GREEN

- The same frontend command passed 26/26.
- The same backend command passed 16/16 with `BUILD SUCCESS`.
- `node --test tests/order-*.test.js` passed 35/35.
- `npm run build` succeeded with Vite 8.1.3 and 1,861 transformed modules.

### Changes

- `logisticsTrackingKey` now includes order ID, shipment ID (or a new-row tracking-number identity), current logistics company, tracking number, and version. The popover Vue key uses the same identity. Behavioral assertions prove changing any identity input produces a new key.
- Backend cache input is now `tenant|order|shipmentId|trimmedCompany|trimmedTrackingNo`.
- `SalesOrderUpdateRequest.shipments` is formally declared as `@Valid @Size(max = 50) List<SalesOrderShipmentSaveRequest>` for `/advance` JSON binding.
- Lifecycle coverage passes a stale request list to `advanceSalesOrderToNextStage`, proves persisted shipments are authoritative, and verifies `saveShipments` is never called by advance.
- `order-flow-behavior.test.js` uses the shipment-array payload model.
- The order table internal key, export branch, generated column CSS, and tests use `shipments`; local table settings moved to `order.list.commercial.v5` so saved `expressNo` layouts cannot survive the rename.

### Self-Review

- `rg` found no `expressNo` or `expressCompany` reference in `management-ui/src/views/function/order/order.vue` or the order-flow behavior fixture.
- Installation-task scalar logistics fields and the tracking response VO were not changed.
- `OrderService.advanceSalesOrderToNextStage` still validates only `orderShipmentService.listShipments(...)`; only full create/save paths call `saveShipments(...)`.
- Cache identity uses the already validated and trimmed `company` and `expressNo` locals.
- `git diff --check` reported no whitespace errors; the repository's existing CRLF conversion warnings remain.

### Review Concerns

- Maven still emits the existing Byte Buddy dynamic-agent/bootstrap warnings.
- Vite reports plugin timing information because terser dominates build time; the build exits successfully.
- The pre-existing `.superpowers/sdd/progress.md` modification remains excluded from this fix commit.

## Concerns

- 无阻塞问题。后端 `/advance` 当前 DTO 不声明 `shipments`，但管理端会先通过完整保存接口持久化物流，再按本任务合同把同一 `shipments` 列表传给推进接口。
- 工作区原有 `.superpowers/sdd/progress.md` 改动不属于本任务，不纳入提交。
