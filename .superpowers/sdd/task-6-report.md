# Task 6 订单多物流代码/文档门禁报告

## 范围

- 分支：`codex/order-multi-shipment`
- 起始 HEAD：`a0a891e2f00caf68555807c158a3cdd17cdfca39`
- 仅执行 Task 6 brief 步骤 1-5；未切换 `main`、未合并、未创建 worktree、未打包或部署。
- 未修改历史迁移、小程序前端或安装任务独立物流字段。

## TDD RED

先新增跨层合同门禁和 `/advance` 参数注解反射测试，再运行：

```powershell
cd D:\HiveManager\management-ui
node --test tests/order-multi-shipment-cross-layer.test.js
```

结果：退出码 `1`，4 项测试中 2 项通过、2 项失败。失败分别命中 `OrderLogisticsTrackingVO.expressNo` 和 `/advance` 请求体缺少 `@Valid`。

```powershell
cd D:\HiveManager\management
.\mvnw.cmd "-Dtest=OrderMultiShipmentLifecycleTest" test
```

结果：`BUILD FAILURE`，11 项测试中 10 项通过、1 项失败；反射断言确认 `/advance` 的 `SalesOrderUpdateRequest` 参数没有 `@Valid`。

## TDD GREEN

- 跨层合同门禁：4/4 通过。
- 定向后端测试：`OrderMultiShipmentLifecycleTest`、`OrderLogisticsTrackingServiceTest`、`Kuaidi100ClientTest` 共 19/19 通过，且执行了 481 个主源码文件和 61 个测试源码文件的 Java 编译。
- tracking response VO、物流服务和快递客户端统一使用 `trackingNo`；未保留 `expressNo` 兼容字段。
- `OrderController` 的 `/advance` 请求体参数补充 `@Valid`，使 `SalesOrderUpdateRequest.shipments` 上的 `@Size(max = 50)` 和嵌套 `@Valid` 进入 MVC 校验链。

## 完整标准验证

```text
management\.\mvnw.cmd test
Tests run: 260, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS

management-ui\npm test
tests 289, pass 289, fail 0, skipped 0

management-ui\npm run build
vite v8.1.3, 1861 modules transformed, built in 11.56s

git diff --check
exit code 0
```

完整标准测试总数：549/549（后端 260 + 管理端 289）。

## 改动

- 新增 `management-ui/tests/order-multi-shipment-cross-layer.test.js`，扫描活动订单 Java/Vue/API 合同、数据库基线和迁移目标，并明确把安装任务作为独立保留域排除。
- tracking response 的 `expressNo` 重命名为 `trackingNo`，同步 `OrderLogisticsTrackingService`、`Kuaidi100Client` 和后端测试。
- `/orders/{orderId}/advance` 请求体增加 `@Valid`，新增控制器参数反射回归测试。
- 更新 `docs/management-ui/modules/order.md`，记录 shipment 子表生命周期、已保存不可删除、乐观锁、shipped 校验、shipment-specific 轨迹、hover-only 查询、成功/失败缓存、company-aware 身份、操作日志及先保存再推进。
- 清理 `.superpowers/sdd/task-5-report.md` 中 tracking VO 和 `/advance` DTO 的过期结论。

## 自审

- 活动订单控制器、模型、服务、快递客户端和管理端订单页面/API 中无 `expressCompany`、`expressNo` 及对应 get/set 残留。
- `SalesOrderSaveRequest` 与 `SalesOrderUpdateRequest` 均保留 `List<SalesOrderShipmentSaveRequest> shipments`；订单表单使用 `orderForm.shipments`；轨迹 API 仅使用 `/orders/{orderId}/shipments/{shipmentId}/logistics-tracking`。
- 基线中的 `sales_order` 不含旧物流列，`sales_order_shipment` 使用 `logistics_company` / `tracking_no`；历史迁移只读未改。
- 安装任务 DTO、实体、服务和管理端页面均未修改，其 `expressCompany` / `expressNo` 独立合同仍由门禁显式确认。
- 未修改小程序源码，未生成或替换发布包，未触碰 `main`。
- 用户已有 `.superpowers/sdd/progress.md` 修改保持未暂存，不纳入本任务提交。

## Review Follow-up: Unified Frontend Contract

- Starting HEAD: `2c873a6` on `codex/order-multi-shipment`; no worktree or branch change.
- TDD RED: `node --test tests/order-multi-shipment-cross-layer.test.js` exited `1` with 3 passing and 1 failing test. The new active-contract assertion failed because `docs/architecture/unified-frontend-contract.md` did not define the `订单物流合同` section.
- TDD GREEN: the same focused test passed 4/4 after the contract documented management UI shipment-list logistics as `shipments[].logisticsCompany` and `shipments[].trackingNo`, prohibited order-level legacy fields, preserved saved shipment records, and specified the shipment-specific tracking route.
- Scope statement: the contract explicitly says the mini-program frontend is not implemented as part of this multi-shipment change and must not be represented as already adapted. It no longer defines the retired top-level order logistics fields as the unified contract.
- Verification: `git diff --check` exited `0`.

## Review Follow-up Concerns

- No blocking concerns. The existing user-owned `.superpowers/sdd/progress.md` modification remains unstaged and excluded from this fix.

## Concerns

- Maven 仍输出既有 Byte Buddy 动态 agent 和 bootstrap classpath 警告，不影响测试成功。
- Vite 构建仍输出 terser 插件耗时提示，不影响构建成功。
- 无阻塞问题。

## Final Review Fix: Clean Launch, Audit, Export, and Permission Gates

### Product decision

- `V20260717_001_order_multi_shipment.sql` is a clean-launch destructive contract. Production deployment must start after old business data is cleared.
- No data is backfilled from `sales_order.express_company` or `sales_order.express_no`; the old columns and compatibility reads are not retained.
- The v2 schema baseline represents `V20260717_001`, and baseline import registers that cutoff before running newer migrations.

### TDD RED

- Focused Maven run: 17 tests executed with 3 failures and 1 error. Shipment events had no `traceId`; the real H2 `operation_log` insert failed its NOT NULL contract; the order audit query excluded `order_shipment`.
- Focused management UI run: 16 tests executed with 6 failures. The v2 baseline and cutoff had not converged, current-page export still used DOM text, and list-only users had no logistics-query gate.

### TDD GREEN

- Focused Maven run: 17/17 passed after shipment events adopted the standard 32-character UUID trace, `INFO` level, zero-duration defaults, and order audit queries included `order_shipment`.
- Focused management UI run: 16/16 passed after baseline/cutoff convergence, structured current-page export, and the `order:detail` popover/request gate.
- Historical migrations, mini-program code, and installation-task logistics were not changed.

### Final verification

- `management\.\mvnw.cmd test`: 262/262 passed, 0 failures, 0 errors, 0 skipped; `BUILD SUCCESS`.
- `management-ui\npm test`: 293/293 passed, 0 failures, 0 skipped.
- `management-ui\npm run build`: 1861 modules transformed; production build succeeded in 10.79s.
- Combined automated tests: 555/555 passed.
- `git diff --check`: exit code 0.
- Non-blocking existing warnings: Maven reports the Byte Buddy dynamic-agent/bootstrap-classpath notice; Vite reports terser plugin timing.
