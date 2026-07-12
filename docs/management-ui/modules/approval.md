# 审批中心

## 源码/路由/批次

- 页面：`management-ui/src/views/function/approval/approvalCenter.vue`。
- 前端 API：`management-ui/src/views/function/approval/api/approval.js`。
- 后端入口：`management/src/main/java/my/management/controller/ApprovalController.java`。
- 服务：`module/approval/service/ApprovalService.java`、`ApprovalDefaultAuditorService.java`。
- 关联域：订单、质量、员工离职、考勤记录和业务附件服务。
- 路由：`/function/approval`；feature 为 `module.approval`。
- 路由权限为请假/财务/离职的列表或提交权限、`order:list`、`badproduct:process` 的并集。
- 迁移批次：Batch 2；当前状态为 Audit baseline。

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
| `list/get/auditQualityApproval`     | GET/POST `/approval/quality/*`              | `badproduct:process`          |
| `listOrderApprovals`                | GET `/approval/order/list`                  | `order:list`                  |
| `getOrderApprovalDetail`            | GET `/approval/order/{type}/{id}`           | `order:detail`                |
| `auditOrderApproval`                | POST `/approval/order/audit`                | `approval:order:audit`        |

## 权限/feature

- `ApprovalController` 整体受 `module.approval` 约束。
- 前端标签权限：订单 `order:list`；质量 `badproduct:process`；财务为列表/提交/审核并集；请假为 `approval:leave`；离职为列表/提交/审核并集。
- 列表加载另按各标签的 `listPermission` 判断；提交账号可进入财务/离职标签但不会请求无权列表。
- 审批按钮同时检查类型审核权限和后端返回的 `canAudit`/审核人 ID。
- 明确差异：路由未列出财务、请假、离职的 `:audit` 权限，只有审核权限的账号可能在路由层被拒绝。
- 明确差异：详情按钮没有检查 `leave/finance/resignation:detail` 或 `order:detail`，列表可见后仍可能收到 403。
- 财务附件下载同样要求 `approval:finance:detail`，当前详情中的下载动作没有独立前端权限门。
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

## 空错态

- 列表有 loading、空表格和筛选后空结果。
- 汇总失败会被转换为全零，当前无法区分“确实无待办”和“汇总请求失败”。
- 标签切换请求失败时 `rows` 不会预先清空，旧类型数据可能停留在新标签下。
- 详情加载没有独立 loading/error 面；失败依赖全局消息且不会打开新详情。
- 审核、提交按钮缺少统一的行级 in-flight 锁，快速重复点击可能并发发请求。
- 附件有上传中状态和 10MB 前端限制；下载使用 blob/object URL。

## 控件现状

- 标签、筛选、表格、分页外观、状态标签和大部分命令为原生元素。
- 详情、默认审核人、财务提交和离职提交为手写弹层/对话框。
- 表格列使用 `TableColumnSettings` 与本地列顺序。
- 审核人选择、输入、日期、金额和 textarea 仍以原生控件为主。
- 消息和部分确认依赖 Element Plus 服务；附件上传/下载保留专用业务处理。

## Element Plus 对照/保留项

- 标签使用 `ElTabs`，但只渲染有权标签并保留待办计数。
- 筛选使用 `ElInput`、`ElSelect`；列表使用 `ElTable`、`ElTag`、`v-loading`、`ElEmpty`。
- 详情与提交表单使用 `ElDialog`，默认负责人可用 `ElDrawer` 或 `ElDialog`。
- 金额、日期、审核人使用 `ElInputNumber`、`ElDatePicker`、`ElSelect`，保持数字和日期格式。
- 命令使用 `ElButton` 并设置真实 loading，权限指令必须挂在最终可点击元素。
- 保留附件 multipart、30 秒超时、blob 下载及文件名处理，不替换业务存储协议。
- 保留订单/质量专用状态文案和领域流转，不把它们简化成通用审批状态机。

## 风险

- 路由、标签、详情、下载和后端存在多处已确认的权限不一致，迁移时不能只复制某一层。
- 顺序重复审批有业务保护：已处理请假/财务/离职会被拒绝，订单/质量要求仍存在待处理领域状态。
- 这些状态检查不等于强并发幂等；请求没有幂等键，前端也没有统一防双击锁。
- 离职提交采用“先查待审批数量再插入”；顺序重复会拦截，但并发提交仍需数据库约束/锁专项验证。
- 财务提交未见客户端请求号；重复点击可能生成两张不同审批单，不能按“同内容”自动去重。
- 审批副作用跨考勤、员工角色、订单和质量域，重复执行或部分失败的回滚边界必须以事务测试确认。
- summary/auditors 仅受 feature 保护，直接调用面的数据可见范围需持续回归。
- 标签请求失败保留旧 rows 会造成跨类型陈旧数据显示，是迁移设计明确禁止的情形。

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
