# 审批中心

## 源码/路由/批次

- 页面：`management-ui/src/views/function/approval/approvalCenter.vue`。
- 前端 API：`management-ui/src/views/function/approval/api/approval.js`。
- 后端入口：`management/src/main/java/my/management/controller/ApprovalController.java`。
- 服务：`module/approval/service/ApprovalService.java`、`ApprovalDefaultAuditorService.java`、`ApprovalAuditorCandidateService.java`。
- 关联域：订单、质量、员工离职、考勤记录和业务附件服务。
- 路由：`/function/approval`；feature 为 `module.approval`。
- 路由权限为请假/财务/离职的列表或提交权限、`order:list`、`quality:process` 的并集。
- 迁移批次：Batch 2；Task 8 已完成 Element Plus 控件迁移。

## 功能

- 展示待审批汇总，并在订单、质量、财务、请假、离职五类标签间切换。
- 按关键字和状态过滤当前类型列表，打开详情。
- 对待处理记录执行快捷通过/拒绝或在详情中填写意见后审批。
- 配置五类审批的默认负责人。
- 新建财务申请，选择审核人并上传、下载附件。
- 新建离职申请，填写预计离职日期、原因、交接说明和审核人。
- 展示“我的申请”“待我审批”“已通过”等统计。

## API 表

| 包装器                              | 方法与路径                                  | 后端权限                      |
| ----------------------------------- | ------------------------------------------- | ----------------------------- |
| `getApprovalSummary`                | GET `/approval/summary`                     | 无方法级权限，仅 feature      |
| `listApprovalAuditors`              | GET `/approval/auditors`                    | 无方法级权限，仅 feature      |
| `listApprovalDefaultAuditors`       | GET `/approval/default-auditors`            | `approval:finance:audit`      |
| `saveApprovalDefaultAuditor`        | POST `/approval/default-auditors`           | `approval:finance:audit`      |
| `listLeaveApprovals`                | GET `/approval/leave/list`                  | `approval:leave`              |
| `getLeaveApprovalDetail`            | GET `/approval/leave/{code}`                | `approval:leave:detail`       |
| `auditLeaveApproval`                | POST `/approval/leave/audit`                | `approval:leave:audit`        |
| `listFinanceApprovals`              | GET `/approval/finance/list`                | `approval:finance`            |
| `getFinanceApprovalDetail`          | GET `/approval/finance/{code}`              | `approval:finance:detail`     |
| `auditFinanceApproval`              | POST `/approval/finance/audit`              | `approval:finance:audit`      |
| `submitFinanceApproval`             | POST `/approval/finance/submit`             | `approval:finance:submit`     |
| `uploadFinanceApprovalAttachment`   | POST `/approval/finance/attachment/upload`  | `approval:finance:submit`     |
| `downloadFinanceApprovalAttachment` | GET `/approval/finance/attachment/download` | `approval:finance:detail`     |
| `listResignationApprovals`          | GET `/approval/resignation/list`            | `approval:resignation`        |
| `getResignationApprovalDetail`      | GET `/approval/resignation/{code}`          | `approval:resignation:detail` |
| `submitResignationApproval`         | POST `/approval/resignation/submit`         | `approval:resignation:submit` |
| `auditResignationApproval`          | POST `/approval/resignation/audit`          | `approval:resignation:audit`  |
| `list/get/auditQualityApproval`     | GET/POST `/approval/quality/*`              | `quality:process`             |
| `listOrderApprovals`                | GET `/approval/order/list`                  | `order:list`                  |
| `getOrderApprovalDetail`            | GET `/approval/order/{type}/{id}`           | `order:detail`                |
| `auditOrderApproval`                | POST `/approval/order/audit`                | 控制器 `approval:list`；服务按订单阶段校验精确审核权限 |

## 权限/feature

- `ApprovalController` 整体受 `module.approval` 约束。
- 订单审批列表入口要求 `approval:list`；服务层再按订单阶段要求 `order:audit:material`、`order:audit:shipment`、`order:audit:cancel` 或 `order:audit:rollback`，旧 `approval:order:audit` 已停用。
- 前端标签权限：订单 `order:list`；质量 `quality:process`；财务为列表/提交/审核并集；请假为 `approval:leave`；离职为列表/提交/审核并集。
- 列表加载另按各标签的 `listPermission` 判断；提交账号可进入财务/离职标签但不会请求无权列表。
- 审批按钮同时检查类型审核权限和后端返回的 `canAudit`/审核人 ID。
- 明确差异：路由未列出财务、请假、离职的 `:audit` 权限，只有审核权限的账号可能在路由层被拒绝。
- 详情命令按 `approval:leave:detail`、`approval:finance:detail`、`approval:resignation:detail` 或 `order:detail` 校验；质量详情使用 `quality:process`。无权限命令保持可见但禁用，并说明原因，处理函数也不会发起请求。
- 财务附件下载要求 `approval:finance:detail`，下载命令和处理函数均设置独立权限门。
- 默认负责人统一借用 `approval:finance:audit`，不是五类独立配置权限。
- summary 和 auditors 无方法级 `@RequirePermission`；不能把前端可见性当成后端授权。

## 状态流

1. 进入页面后加载汇总，并加载首个有权标签的列表。
2. 请假、财务、离职使用数字状态：`1 待审批 -> 2 已通过 / 3 已拒绝`。
3. 质量和订单审批映射各自领域状态，不应强制改写为通用数字状态。
4. 审批前后端还校验当前审核人；不是配置审核人的用户不能仅凭列表权限处理。
5. 请假通过会联动考勤数据；离职通过会把员工标记离职并撤销角色。
6. 订单通过/拒绝推进或回退订单领域状态；质量审核推进质量处理状态。
7. 财务/离职提交生成审批编号，成功后切换相应标签并刷新列表。
8. 默认审核人保存后刷新配置；具体申请仍可提交显式审核人集合。
9. 多审核人审批写入 `approval_auditor_candidate`；服务在同一事务内锁定候选集合、原子记录当前审核人的决定，再返回 `PENDING/APPROVED/REJECTED` 汇总结果。只有全部通过才执行领域推进，任一驳回立即关闭本次候选集合。
10. 创建/保存待收款订单不会创建审批候选；只有显式执行 `pending_pay -> pending_material` 才创建备料审批。`pending_ship -> shipped` 继续走发货审批；驳回时保持原订单状态。
11. 审批中心订单列表只展示存在有效候选的记录；GET 列表和详情不补建候选、不产生写操作。

## 空错态

- 列表将 loading、本地无权限、持久失败、成功真空/数据互斥渲染；失败态提供重试。
- 汇总失败会被转换为全零，当前无法区分“确实无待办”和“汇总请求失败”。
- 每次列表请求开始立即清空 `rows`；401、403、网络错误和 5xx 使用不同持久错误状态。
- 标签快速切换使用递增 request id，只允许最后一次请求提交数据或错误，旧响应不会覆盖当前标签。
- 详情打开前清空旧内容，独立呈现 loading、真实空态、401/403、网络/5xx 失败并支持重试；递增 request-id 只允许最后一次详情请求更新界面。
- 审核、提交按钮缺少统一的行级 in-flight 锁，快速重复点击可能并发发请求。
- 附件有上传中状态和 10MB 前端限制；下载使用 blob/object URL。

## Element Plus 控件与保留项

- 详情命令按 `approval:leave:detail`、`approval:finance:detail`、`approval:resignation:detail`、`order:detail`（质量使用 `quality:process`）保持可见但禁用并说明原因；财务附件下载按 `approval:finance:detail` 同步保护。
- 详情抽屉在请求前清空旧内容，独立呈现 loading、empty、401/403、网络/5xx 失败并可重试；request-id 防止跨审批旧响应和旧 finally 覆盖。

- 五类标签使用 `ElTabs`、`ElTabPane` 和 `ElBadge`，禁用状态继续由原权限矩阵计算。
- 筛选使用 `ElInput`、`ElSelect`；列表使用 `ElTable`、`ElTableColumn`、`ElTag`、`ElPagination`、`v-loading` 和 `ElEmpty`。
- 详情使用 `ElDialog`、`ElDescriptions`；审核、财务和离职输入使用 `ElForm`、`ElFormItem`、`ElInput`。
- 金额、日期、审核人分别使用 `ElInputNumber`、`ElDatePicker`、`ElSelect`，日期提交格式保持 `YYYY-MM-DD`。
- 所有 Element Plus 组件均在页面中显式导入；权限指令仍挂在最终可点击的 `ElButton` 上。
- 表格列继续使用 `TableColumnSettings` 与本地列顺序，分页仅作用于当前筛选结果。
- 保留附件 multipart、30 秒超时、blob 下载及文件名处理，不替换业务存储协议。
- 保留订单/质量专用状态文案和领域流转，不把它们简化成通用审批状态机。

## 风险

- 路由、标签、详情、下载和后端存在多处已确认的权限不一致，迁移时不能只复制某一层。
- 顺序重复审批有业务保护：已处理请假/财务/离职会被拒绝，订单/质量要求仍存在待处理领域状态。
- 候选审批决定采用数据库行锁和 `audit_status = 0` 条件更新，可防止同一审核人重复决定以及多人最后一步重复执行；前端仍需补行级 in-flight 锁改善重复点击反馈。
- 离职提交采用“先查待审批数量再插入”；顺序重复会拦截，但并发提交仍需数据库约束/锁专项验证。
- 财务提交未见客户端请求号；重复点击可能生成两张不同审批单，不能按“同内容”自动去重。
- 审批副作用跨考勤、员工角色、订单和质量域，重复执行或部分失败的回滚边界必须以事务测试确认。
- summary/auditors 仅受 feature 保护，直接调用面的数据可见范围需持续回归。
- 列表 last-request-wins 只阻止旧响应落地，并不会取消已发出的旧请求；后端仍应保持列表查询无副作用。

## 验证

- [ ] 建立路由、标签、列表、详情、审核、提交、附件下载的权限组合矩阵。
- [ ] 用“仅审核”“仅提交”“仅列表”“仅详情”账号验证前后端一致性。
- [ ] 验证审核人身份限制、默认审核人配置和 `canAudit`。
- [ ] 对同一审批连续提交、双击并发和两浏览器并发，验证幂等与副作用只执行一次。
- [ ] 并发提交同一离职申请，验证最多一张待审批单。
- [ ] 验证请假、离职、订单、质量通过/拒绝后的关联领域状态。
- [ ] 验证标签请求失败、汇总失败、详情失败时不显示旧类型数据或伪零空态。
- [ ] 验证附件上传上限、失败重试、授权下载、文件名和 object URL 释放。
- [ ] 验证窄屏表格、长意见、键盘焦点、Escape 和关闭后的焦点恢复。
- [ ] 运行目标测试、lint、生产构建，确认 API、权限码和领域状态未改变。
